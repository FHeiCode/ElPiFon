package de.elpifon.app.call

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.TelecomManager
import de.elpifon.app.data.CallerLookup
import de.elpifon.app.data.ContactStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Wird von Android gestartet, sobald ElPiFon die Standard-Telefon-App ist.
 * Android meldet hier jeden ein- und ausgehenden Anruf; das Klingeln übernimmt das System.
 */
class ElInCallService : InCallService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        val store = ContactStore(applicationContext)
        scope.launch { store.contacts.collect { CallManager.contacts.value = it } }
        scope.launch { store.settings.collect { CallManager.settings.value = it } }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        val ringing = CallManager.stateOf(call) == Call.STATE_RINGING

        // Kein Anklopfen: Während eines Gesprächs hört der Anrufer „besetzt“.
        if (ringing && CallManager.isBusy()) {
            call.reject(false, null)
            return
        }
        CallManager.add(call)

        if (ringing && CallManager.settings.value.silenceUnknown) {
            val number = CallManager.numberOf(call)
            if (!CallerLookup.isKnown(this, CallManager.contacts.value, number)) {
                runCatching { getSystemService(TelecomManager::class.java)?.silenceRinger() }
            }
        }

        // Anruf-Bildschirm zeigen (auch über dem Sperrbildschirm).
        runCatching {
            startActivity(
                Intent(this, CallActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            )
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        CallManager.remove(call)
    }
}
