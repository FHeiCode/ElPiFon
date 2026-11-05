package de.elpifon.app.call

import android.telecom.Call
import android.telecom.VideoProfile
import de.elpifon.app.data.AppSettings
import de.elpifon.app.data.Contact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Was der Anruf-Bildschirm über das aktuelle Gespräch wissen muss. */
data class CallUi(
    val call: Call,
    /** Eine der Call.STATE_*-Konstanten. */
    val state: Int,
    /** Telefonnummer, leer bei unterdrückter Nummer. */
    val number: String,
) {
    val isRinging get() = state == Call.STATE_RINGING
}

/**
 * Zentrale Stelle für das laufende Gespräch. Bewusst einfach: Es gibt immer nur
 * ein Gespräch – ein zweiter Anruf während eines Gesprächs wird abgewiesen (kein Anklopfen).
 */
object CallManager {
    private var call: Call? = null

    private val _current = MutableStateFlow<CallUi?>(null)
    val current: StateFlow<CallUi?> = _current.asStateFlow()

    /** Vom Dienst laufend aktualisiert, damit Anrufer-Name und Foto sofort bekannt sind. */
    val contacts = MutableStateFlow<List<Contact>>(emptyList())
    val settings = MutableStateFlow(AppSettings())

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) = refresh()
        override fun onDetailsChanged(call: Call, details: Call.Details) = refresh()
    }

    /** Läuft schon ein Gespräch? Dann wird ein weiterer Anruf nicht angenommen. */
    fun isBusy(): Boolean = call?.let { stateOf(it) != Call.STATE_DISCONNECTED } ?: false

    fun add(newCall: Call) {
        call?.unregisterCallback(callback)
        call = newCall
        newCall.registerCallback(callback)
        refresh()
    }

    fun remove(removed: Call) {
        if (removed != call) return
        removed.unregisterCallback(callback)
        call = null
        refresh()
    }

    @Suppress("DEPRECATION")
    fun stateOf(call: Call): Int = call.state

    fun numberOf(call: Call): String = call.details?.handle?.schemeSpecificPart.orEmpty()

    private fun refresh() {
        _current.value = call?.let {
            CallUi(
                call = it,
                state = stateOf(it),
                number = numberOf(it),
            )
        }
    }

    fun answer() {
        call?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun hangUp() {
        call?.disconnect()
    }
}
