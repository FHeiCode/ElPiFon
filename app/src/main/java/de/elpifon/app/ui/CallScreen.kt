package de.elpifon.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.elpifon.app.call.CallUi
import de.elpifon.app.data.CallerLookup
import de.elpifon.app.data.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Green = Color(0xFF2E7D32)

/**
 * Anruf-Bildschirm: nur Foto, Name und große Tasten.
 * Klingelt es → Ablehnen/Annehmen, im Gespräch → Auflegen, danach schließt er sich.
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
            call.isRinging -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BigCallButton("Ablehnen", Icons.Filled.CallEnd, Signal.emergency, Modifier.weight(1f), onReject)
                BigCallButton("Annehmen", Icons.Filled.Call, Green, Modifier.weight(1f), onAnswer)
            }
            else -> BigCallButton("Auflegen", Icons.Filled.CallEnd, Signal.emergency, Modifier.fillMaxWidth(), onHangUp)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun BigCallButton(label: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Raised3D(
        color = color,
        pressed = pressed,
        corner = 28.dp,
        modifier = modifier
            .height(136.dp)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { contentDescription = label; role = Role.Button },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(52.dp))
            Text(
                label,
                color = Color.White,
                // Feste Größe, unabhängig von der Schriftgröße des Handys – passt immer in die Taste.
                fontSize = with(LocalDensity.current) { 26.dp.toSp() },
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                style = TextStyle(shadow = TextOnColorShadow),
            )
        }
    }
}
