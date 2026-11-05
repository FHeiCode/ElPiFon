package de.elpifon.app.call

import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.telecom.Call
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.elpifon.app.ui.CallScreen
import de.elpifon.app.ui.ElPiFonTheme
import kotlinx.coroutines.delay

/** Vollbild für eingehende und laufende Gespräche – auch über dem Sperrbildschirm. */
class CallActivity : ComponentActivity() {
    private var proximityLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        setContent {
            ElPiFonTheme {
                val current by CallManager.current.collectAsStateWithLifecycle()
                val contacts by CallManager.contacts.collectAsStateWithLifecycle()
                // Letzter bekannter Anruf, damit „Gespräch beendet“ noch Name/Foto zeigen kann.
                var last by remember { mutableStateOf(current) }

                LaunchedEffect(current) {
                    val c = current
                    if (c != null) last = c
                    val ended = c == null || c.state == Call.STATE_DISCONNECTED
                    setProximityLock(!ended && !c!!.isRinging)
                    if (ended) {
                        delay(1800)
                        if (CallManager.current.value == null) finish()
                    }
                }

                val shown = current ?: last
                if (shown != null) {
                    CallScreen(
                        call = shown,
                        ended = current == null || current?.state == Call.STATE_DISCONNECTED,
                        contacts = contacts,
                        onAnswer = CallManager::answer,
                        onReject = CallManager::reject,
                        onHangUp = CallManager::hangUp,
                    )
                }
            }
        }
    }

    /** Bildschirm aus, solange das Handy am Ohr ist (verhindert Tippen mit der Wange). */
    private fun setProximityLock(on: Boolean) {
        val pm = getSystemService(PowerManager::class.java) ?: return
        if (on) {
            if (proximityLock?.isHeld == true) return
            if (!pm.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) return
            proximityLock = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "elpifon:call").apply {
                setReferenceCounted(false)
                acquire(4 * 60 * 60 * 1000L)
            }
        } else {
            proximityLock?.let { if (it.isHeld) it.release() }
            proximityLock = null
        }
    }

    override fun onDestroy() {
        setProximityLock(false)
        super.onDestroy()
    }
}
