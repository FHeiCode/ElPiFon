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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
        Spacer(Modifier.weight(0.6f))
        ContactAvatar(title, contact?.photo, size = 200.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 46.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (call.isRinging && !ended) {
            Text(
                text = "ruft an",
                fontSize = 34.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.weight(1f))

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
    Surface(
        onClick = onClick,
        color = color,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .height(130.dp)
            .semantics { contentDescription = label; role = Role.Button },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
            Text(label, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
    }
}
