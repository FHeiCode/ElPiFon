package de.elpifon.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import de.elpifon.app.R
import androidx.compose.material3.IconButton
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.elpifon.app.data.Contact
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** So lange muss der Notruf-Knopf gehalten werden – verhindert versehentliches Auslösen. */
private const val HOLD_SECONDS = 2

@Composable
fun HomeScreen(
    contacts: List<Contact>?,
    onCall: (Contact) -> Unit,
    onEmergency: () -> String,
    onOpenSettings: () -> Unit,
    /** Läuft gerade ein Gespräch, führt ein grüner Balken zurück zum Anruf-Bildschirm. */
    ongoingCall: Boolean = false,
    onReturnToCall: () -> Unit = {},
) {
    var hint by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(hint) {
        if (hint != null) {
            delay(6000)
            hint = null
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Header()
            SettingsMenu(onClick = onOpenSettings, modifier = Modifier.align(Alignment.TopEnd))
        }

        if (ongoingCall) {
            Text(
                text = "Gespräch läuft – hier tippen",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(androidx.compose.ui.graphics.Color(0xFF2E7D32))
                    .clickable(onClick = onReturnToCall)
                    .padding(16.dp),
            )
        }

        hint?.let {
            Text(
                text = it,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Signal.onHint,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Signal.hint)
                    .padding(14.dp),
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 10.dp)) {
            when {
                contacts == null -> Unit // lädt noch
                contacts.isEmpty() -> Text(
                    text = "Noch keine Kontakte.\n\nFür Angehörige: oben rechts auf ⋮ " +
                        "tippen und Kontakte hinzufügen.",
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(contacts, key = { it.id }) { contact ->
                        ContactTile(contact) {
                            hint = "${contact.name} wird angerufen …"
                            onCall(contact)
                        }
                    }
                }
            }
        }

        EmergencyButton(
            onTrigger = { hint = onEmergency() },
            onShortTap = { hint = "Für einen Notruf den roten Knopf $HOLD_SECONDS Sekunden gedrückt halten." },
        )
    }
}

/** Kopfzeile, linksbündig: rundes Logo, Schriftzug „ElPiFon“ und klein „by Frank“ – unten bündig. */
@Composable
private fun Header() {
    Row(
        // Rechts Platz lassen für die drei Punkte.
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp, end = 48.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_round),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            painter = painterResource(R.drawable.elpi_wordmark),
            contentDescription = "ElPiFon",
            // Schwarz (im dunklen Modus weiß, damit es lesbar bleibt).
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.height(44.dp).width(155.dp),
        )
        Text(
            text = "by Frank",
            fontSize = 16.sp,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            // Grundlinie auf Höhe der Unterkante des Schriftzugs.
            modifier = Modifier.padding(start = 6.dp).offset(y = 3.dp),
        )
    }
}

@Composable
private fun ContactTile(contact: Contact, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .semantics { contentDescription = "${contact.name} anrufen" },
    ) {
        Column(
            Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ContactAvatar(contact.name, contact.photo, size = 130.dp)
            Spacer(Modifier.height(10.dp))
            Text(
                text = contact.name,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 26.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun EmergencyButton(onTrigger: () -> Unit, onShortTap: () -> Unit) {
    val trigger by rememberUpdatedState(onTrigger)
    val shortTap by rememberUpdatedState(onShortTap)
    var remaining by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Signal.emergency)
            .semantics {
                contentDescription = "Notruf. $HOLD_SECONDS Sekunden gedrückt halten."
                role = Role.Button
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    var fired = false
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val countdown = scope.launch {
                        for (s in HOLD_SECONDS downTo 1) {
                            remaining = s
                            delay(1000)
                        }
                        remaining = null
                        fired = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        trigger()
                    }
                    tryAwaitRelease()
                    if (!fired) {
                        countdown.cancel()
                        remaining = null
                        shortTap()
                    }
                })
            },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = remaining?.let { "Weiter halten … $it" } ?: "NOTRUF",
                color = Signal.onEmergency,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "$HOLD_SECONDS Sekunden gedrückt halten",
                color = Signal.onEmergency,
                fontSize = 18.sp,
            )
        }
    }
}

/** Drei Punkte oben rechts: öffnet die Einstellungen (geschützt durch die PIN, falls vergeben). */
@Composable
private fun SettingsMenu(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier.size(48.dp)) {
        Icon(
            Icons.Filled.MoreVert,
            contentDescription = "Einstellungen",
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            modifier = Modifier.size(28.dp),
        )
    }
}
