package de.elpifon.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.elpifon.app.call.CallUi
import de.elpifon.app.data.CallerLookup
import de.elpifon.app.data.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Green = Color(0xFF2E7D32)

/**
 * Anruf-Bildschirm: nur Foto, Name und große Tasten.
 * Klingelt es → großes Annehmen und kleines Ablehnen (gedrückt halten), im Gespräch → Auflegen, danach schließt er sich.
 */
@Composable
fun CallScreen(
    call: CallUi,
    ended: Boolean,
    contacts: List<Contact>,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    onHangUp: () -> Unit,
) {
    val context = LocalContext.current
    val contact = CallerLookup.findContact(contacts, call.number)
    val phonebookName by produceState<String?>(null, call.number, contact) {
        value = if (contact != null) null
        else withContext(Dispatchers.IO) { CallerLookup.phonebookName(context, call.number) }
    }
    val name = contact?.name ?: phonebookName
    val title = when {
        name != null -> name
        call.number.isBlank() -> "Unbekannte Nummer"
        else -> call.number
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Oben Foto und Name. Dieser Teil passt sich dem freien Platz an, damit die Tasten
        // unten immer sichtbar bleiben – auch bei großer Schrift oder Anzeigegröße.
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val avatar = minOf(200.dp, maxHeight * 0.45f, maxWidth * 0.6f)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (avatar >= 48.dp) {
                    ContactAvatar(title, contact?.photo, size = avatar)
                    Spacer(Modifier.height(16.dp))
                }
                Text(
                    text = title,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 46.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (call.isRinging && !ended) {
                    Text(
                        text = "ruft an",
                        fontSize = 34.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        when {
            ended -> Unit
            call.isRinging -> {
                // Annehmen ist die einzige große Fläche. Ablehnen ist bewusst klein, grau und
                // nur durch langes Drücken auszulösen – so lehnt niemand aus Versehen ab.
                BigCallButton("Annehmen", Icons.Filled.Call, Green, Modifier.fillMaxWidth(), onAnswer, height = 180.dp, big = true)
                Spacer(Modifier.height(28.dp))
                HoldToRejectButton(onReject)
            }
            else -> BigCallButton("Auflegen", Icons.Filled.CallEnd, Signal.emergency, Modifier.fillMaxWidth(), onHangUp)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun BigCallButton(
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit,
    height: Dp = 136.dp,
    big: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Raised3D(
        color = color,
        pressed = pressed,
        corner = 28.dp,
        modifier = modifier
            .height(height)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { contentDescription = label; role = Role.Button },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(if (big) 72.dp else 52.dp))
            Text(
                label,
                color = Color.White,
                // Feste Größe, unabhängig von der Schriftgröße des Handys – passt immer in die Taste.
                fontSize = with(LocalDensity.current) { (if (big) 36.dp else 26.dp).toSp() },
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                style = TextStyle(shadow = TextOnColorShadow),
            )
        }
    }
}

private const val REJECT_HOLD_SECONDS = 2

/**
 * Kleine graue Taste „Ablehnen“: löst erst nach [REJECT_HOLD_SECONDS] Sekunden Gedrückthalten aus –
 * genauso wie der Notruf-Knopf. Kurzes Antippen tut nichts.
 */
@Composable
private fun HoldToRejectButton(onReject: () -> Unit) {
    var remaining by remember { mutableStateOf<Int?>(null) }
    val reject by rememberUpdatedState(onReject)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val fg = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)

    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(2.dp, if (remaining != null) fg else fg.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .semantics {
                contentDescription = "Ablehnen. $REJECT_HOLD_SECONDS Sekunden gedrückt halten."
                role = Role.Button
                // Mit TalkBack genügt Doppeltippen.
                onClick(label = "Ablehnen") { reject(); true }
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    var fired = false
                    val countdown = scope.launch {
                        for (s in REJECT_HOLD_SECONDS downTo 1) {
                            remaining = s
                            delay(1000)
                        }
                        remaining = null
                        fired = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        reject()
                    }
                    tryAwaitRelease()
                    if (!fired) {
                        countdown.cancel()
                        remaining = null
                    }
                })
            }
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = remaining?.let { "Weiter halten … $it" } ?: "Ablehnen",
            color = fg,
            fontSize = with(LocalDensity.current) { 22.dp.toSp() },
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        Text(
            text = "$REJECT_HOLD_SECONDS Sekunden gedrückt halten",
            color = fg,
            fontSize = with(LocalDensity.current) { 15.dp.toSp() },
            maxLines = 1,
        )
    }
}
