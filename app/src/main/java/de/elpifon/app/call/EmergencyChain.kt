package de.elpifon.app.call

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import de.elpifon.app.data.PhoneActions

/**
 * Notruf-Kette: ruft die Notfallkontakte der Reihe nach an. Wird ein Anruf nicht innerhalb
 * von einer Minute angenommen, legt ElPiFon auf und ruft den Nächsten an. Sobald jemand
 * abnimmt, endet die Kette und das Gespräch läuft normal weiter.
 *
 * Funktioniert nur, wenn ElPiFon die Telefon-App ist – sonst sieht ElPiFon nicht,
 * ob jemand abgenommen hat. Dann wird nur der erste Notfallkontakt angerufen.
 */
object EmergencyChain {
    private const val TIMEOUT_MS = 60_000L
    /** Kurze Pause zwischen zwei Anrufen. */
    private const val GAP_MS = 2_000L
    /** Erscheint nach dem Wählen gar kein Anruf (z. B. kein Netz), geht es weiter. */
    private const val NO_CALL_MS = 10_000L

    private val handler = Handler(Looper.getMainLooper())
    private var app: Context? = null
    private var numbers: List<String> = emptyList()
    private var index = -1
    private var current: Call? = null

    val isActive: Boolean get() = index >= 0

    private val timeout = Runnable { current?.disconnect() } // → onCallRemoved → nächster
    private val noCall = Runnable { next() }

    /** Startet die Kette. Gibt false zurück, wenn ElPiFon nicht die Telefon-App ist. */
    fun start(context: Context, numbers: List<String>): Boolean {
        if (numbers.isEmpty() || !PhoneActions.isDefaultDialer(context)) return false
        stop()
        app = context.applicationContext
        this.numbers = numbers
        index = 0
        dial()
        return true
    }

    /** Beendet die Kette (z. B. wenn jemand abgenommen oder selbst aufgelegt hat). */
    fun stop() {
        handler.removeCallbacksAndMessages(null)
        index = -1
        numbers = emptyList()
        current = null
    }

    private fun dial() {
        val ctx = app ?: return stop()
        current = null
        handler.postDelayed(noCall, NO_CALL_MS)
        PhoneActions.call(ctx, numbers[index])
    }

    private fun next() {
        handler.removeCallbacksAndMessages(null)
        current = null
        index++
        if (index >= numbers.size) {
            stop() // Alle versucht – keiner hat abgenommen.
            return
        }
        handler.postDelayed({ if (isActive) dial() }, GAP_MS)
    }

    // --- Meldungen vom CallManager ---------------------------------------------------------

    fun onCallAdded(call: Call) {
        if (!isActive) return
        if (CallManager.stateOf(call) == Call.STATE_RINGING) {
            // Jemand ruft an (z. B. ein Angehöriger ruft zurück) – Kette beenden.
            stop()
            return
        }
        current = call
        handler.removeCallbacks(noCall)
        handler.postDelayed(timeout, TIMEOUT_MS)
    }

    fun onStateChanged(call: Call, state: Int) {
        if (isActive && call == current && state == Call.STATE_ACTIVE) stop() // abgenommen
    }

    fun onCallRemoved(call: Call) {
        if (isActive && call == current) next() // nicht abgenommen, besetzt oder nach 1 Minute aufgelegt
    }
}
