package de.elpifon.app.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.telecom.TelecomManager
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.util.Locale

/** Anrufen, Notruf-SMS, Standort und Startbildschirm-Modus. */
object PhoneActions {

    private fun granted(context: Context, permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /** Nur Ziffern, + * # behalten – Leerzeichen und Striche aus dem Telefonbuch stören beim Wählen. */
    fun clean(number: String) = number.filter { it.isDigit() || it in "+*#" }

    /**
     * Ruft direkt an, wenn die Berechtigung erteilt ist; sonst öffnet sich die Wähl-App
     * mit der Nummer, und man muss nur noch auf den grünen Hörer tippen.
     * Ist ElPiFon selbst die Telefon-App, läuft der Anruf direkt über Android (Telecom) –
     * das geht dann auch für die 112.
     */
    @SuppressLint("MissingPermission")
    fun call(context: Context, number: String): Boolean {
        val uri = Uri.fromParts("tel", clean(number), null)
        if (isDefaultDialer(context)) {
            val placed = runCatching {
                context.getSystemService(TelecomManager::class.java)!!.placeCall(uri, Bundle())
            }.isSuccess
            if (placed) return true
            // Ohne Anruf-Berechtigung würde ACTION_DIAL wieder bei ElPiFon landen → App-Einstellungen öffnen.
            Toast.makeText(context, "Bitte die Berechtigung „Telefon“ erlauben.", Toast.LENGTH_LONG).show()
            openAppDetails(context)
            return false
        }
        val action = if (granted(context, Manifest.permission.CALL_PHONE)) Intent.ACTION_CALL else Intent.ACTION_DIAL
        return runCatching { context.startActivity(Intent(action, uri)) }.isSuccess
    }

    // --- Standard-Telefon-App ----------------------------------------------------------------

    fun isDefaultDialer(context: Context): Boolean =
        runCatching { context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage == context.packageName }
            .getOrDefault(false)

    /** Intent, der Android fragt, ob ElPiFon die Standard-Telefon-App werden soll. */
    fun requestDefaultDialerIntent(context: Context): Intent? =
        if (Build.VERSION.SDK_INT >= 29) {
            context.getSystemService(RoleManager::class.java)
                ?.takeIf { it.isRoleAvailable(RoleManager.ROLE_DIALER) }
                ?.createRequestRoleIntent(RoleManager.ROLE_DIALER)
        } else {
            @Suppress("DEPRECATION")
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
        }

    /** Android-Seite „Standard-Apps“, um die Telefon-App wieder zurückzustellen. */
    fun openDefaultAppsSettings(context: Context) {
        runCatching { context.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) }
            .onFailure { openAndroidSettings(context) }
    }

    fun openAppDetails(context: Context) {
        runCatching {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    /**
     * Notruf ohne Notfallkontakt: Ist ElPiFon die Telefon-App, wird die 112 direkt angerufen.
     * Sonst öffnet sich die normale Telefon-App mit der 112 (Apps dürfen sie nicht selbst wählen).
     * Gibt true zurück, wenn direkt angerufen wird.
     */
    fun callEmergencyServices(context: Context): Boolean {
        if (isDefaultDialer(context) && call(context, "112")) return true
        runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", "112", null))) }
        return false
    }

    /**
     * Notruf: SMS an alle Notfallkontakte (mit Standort, falls erlaubt), danach Anruf beim
     * ersten Notfallkontakt. Gibt einen kurzen Text zurück, was passiert ist.
     */
    fun triggerEmergency(context: Context, contacts: List<Contact>, settings: AppSettings): String {
        val targets = contacts.filter { it.emergency }
        if (targets.isEmpty()) {
            return if (callEmergencyServices(context)) "Kein Notfallkontakt eingerichtet – die 112 wird angerufen."
            else "Kein Notfallkontakt eingerichtet – die 112 ist vorgewählt."
        }

        var smsSent = false
        if (granted(context, Manifest.permission.SEND_SMS)) {
            val location = if (settings.sendLocation) lastLocation(context) else null
            val text = buildString {
                append("NOTRUF: ").append(settings.emergencyText)
                if (location != null) {
                    val ageMin = (System.currentTimeMillis() - location.time) / 60_000
                    append("\nStandort")
                    if (ageMin > 10) append(" (vor ").append(ageMin).append(" Min.)")
                    append(": ").append(mapsLink(location))
                }
            }
            smsSent = targets.map { sendSms(context, it.number, text) }.any { it }

            // Ist der bekannte Standort alt oder fehlt er, wird ein aktueller gesucht und nachgeschickt.
            if (settings.sendLocation && (location == null || System.currentTimeMillis() - location.time > 10 * 60_000)) {
                requestFreshLocation(context) { fresh ->
                    targets.forEach { sendSms(context, it.number, "NOTRUF – aktueller Standort: ${mapsLink(fresh)}") }
                }
            }
        }

        call(context, targets.first().number)
        val names = targets.joinToString { it.name }
        return if (smsSent) "Notruf-SMS an $names gesendet. ${targets.first().name} wird angerufen."
        else "${targets.first().name} wird angerufen. (SMS nicht erlaubt)"
    }

    private fun mapsLink(l: Location) =
        String.format(Locale.US, "https://maps.google.com/?q=%.5f,%.5f", l.latitude, l.longitude)

    private fun sendSms(context: Context, number: String, text: String): Boolean = runCatching {
        val sms: SmsManager = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        sms.sendMultipartTextMessage(clean(number), null, sms.divideMessage(text), null, null)
    }.isSuccess

    private fun hasLocationPermission(context: Context) =
        granted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
            granted(context, Manifest.permission.ACCESS_COARSE_LOCATION)

    @SuppressLint("MissingPermission")
    private fun lastLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null
        val lm = context.getSystemService(LocationManager::class.java) ?: return null
        return lm.getProviders(true)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    /** Sucht bis zu 60 Sekunden lang einen aktuellen Standort (ab Android 11). */
    @SuppressLint("MissingPermission")
    private fun requestFreshLocation(context: Context, onFound: (Location) -> Unit) {
        if (Build.VERSION.SDK_INT < 30 || !hasLocationPermission(context)) return
        val lm = context.getSystemService(LocationManager::class.java) ?: return
        val provider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstOrNull { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) } ?: return
        val cancel = CancellationSignal()
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({ cancel.cancel() }, 60_000)
        runCatching {
            lm.getCurrentLocation(provider, cancel, ContextCompat.getMainExecutor(context)) { loc ->
                handler.removeCallbacksAndMessages(null)
                if (loc != null) onFound(loc)
            }
        }
    }

    // --- Startbildschirm-Modus -------------------------------------------------------------

    private fun homeAlias(context: Context) = ComponentName(context.packageName, "de.elpifon.app.HomeAlias")

    fun isHomeModeEnabled(context: Context): Boolean =
        context.packageManager.getComponentEnabledSetting(homeAlias(context)) ==
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED

    fun setHomeMode(context: Context, enabled: Boolean) {
        context.packageManager.setComponentEnabledSetting(
            homeAlias(context),
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }

    /** Öffnet die Android-Auswahl für den Standard-Startbildschirm. */
    fun openHomeSettings(context: Context) {
        runCatching { context.startActivity(Intent(android.provider.Settings.ACTION_HOME_SETTINGS)) }
            .onFailure { runCatching { context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) } }
    }

    fun openAndroidSettings(context: Context) {
        runCatching { context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) }
    }
}
