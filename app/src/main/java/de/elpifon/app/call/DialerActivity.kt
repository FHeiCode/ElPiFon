package de.elpifon.app.call

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import de.elpifon.app.MainActivity
import de.elpifon.app.data.PhoneActions

/**
 * Unsichtbar, ohne eigenen Bildschirm. Android verlangt von jeder Telefon-App, dass sie
 * „Nummer wählen“-Anfragen anderer Apps annimmt:
 * - mit Nummer (z. B. Telefonnummer auf einer Webseite angetippt) → wird direkt angerufen
 * - ohne Nummer → ElPiFon-Startbildschirm
 */
class DialerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val number = numberFrom(intent)
        val mayCall = PhoneActions.isDefaultDialer(this) ||
            checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

        if (number.any { it.isDigit() } && mayCall) {
            PhoneActions.call(this, number)
        } else {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        finish()
    }

    private fun numberFrom(intent: Intent?): String {
        val data: Uri = intent?.data ?: return ""
        return if (data.scheme == "tel") Uri.decode(data.schemeSpecificPart.orEmpty()) else ""
    }
}
