package de.elpifon.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.elpifon.app.data.AppSettings
import de.elpifon.app.data.Contact
import de.elpifon.app.data.PhoneActions
import kotlinx.coroutines.delay

/** Einstellungen für Angehörige: Kontakte pflegen, Notruf, PIN, Startbildschirm. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    contacts: List<Contact>,
    settings: AppSettings,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Contact) -> Unit,
    onMove: (Contact, Int) -> Unit,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit,
) {
    val context = LocalContext.current
    var homeMode by remember { mutableStateOf(PhoneActions.isHomeModeEnabled(context)) }
    var isDialer by remember { mutableStateOf(PhoneActions.isDefaultDialer(context)) }
    val dialerRequest = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDialer = PhoneActions.isDefaultDialer(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Section("Kontakte auf dem Startbildschirm") }
            if (contacts.isEmpty()) {
                item { Text("Noch keine Kontakte.", fontSize = 18.sp) }
            }
            itemsIndexed(contacts, key = { _, c -> c.id }) { index, contact ->
                ContactRow(
                    contact = contact,
                    canMoveUp = index > 0,
                    canMoveDown = index < contacts.lastIndex,
                    onEdit = { onEdit(contact) },
                    onMove = { delta -> onMove(contact, delta) },
                )
            }
            item {
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Kontakt hinzufügen", fontSize = 18.sp)
                }
            }
            item {
                Text(
                    "Notfallkontakte (⚠) bekommen beim Notruf eine SMS; der oberste wird angerufen.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }

            item { Section("Notruf") }
            item {
                DebouncedTextField(
                    label = "Text der Notruf-SMS",
                    value = settings.emergencyText,
                    singleLine = false,
                    onCommit = { text -> onSettingsChange { it.copy(emergencyText = text.ifBlank { AppSettings.DEFAULT_EMERGENCY_TEXT }) } },
                )
            }
            item {
                SwitchRow(
                    title = "Standort mitsenden",
                    subtitle = "Link zu Google Maps in der SMS",
                    checked = settings.sendLocation,
                    onChange = { on -> onSettingsChange { it.copy(sendLocation = on) } },
                )
            }

            item { Section("Schutz") }
            item {
                DebouncedTextField(
                    label = "PIN für die Einstellungen (leer = keine)",
                    value = settings.pin,
                    singleLine = true,
                    keyboardType = KeyboardType.NumberPassword,
                    onCommit = { pin -> onSettingsChange { it.copy(pin = pin.filter(Char::isDigit)) } },
                )
            }

            item { Section("Anrufe") }
            item {
                Text(
                    if (isDialer) "ElPiFon ist die Telefon-App: Anrufe erscheinen mit großen Tasten zum Annehmen und Auflegen."
                    else "Eingehende Anrufe zeigt bisher die normale Telefon-App. Als Telefon-App zeigt ElPiFon sie mit großen Tasten.",
                    fontSize = 16.sp,
                )
            }
            item {
                if (isDialer) {
                    OutlinedButton(onClick = { PhoneActions.openDefaultAppsSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Wieder andere Telefon-App verwenden")
                    }
                } else {
                    Button(
                        onClick = {
                            PhoneActions.requestDefaultDialerIntent(context)?.let { dialerRequest.launch(it) }
                                ?: PhoneActions.openDefaultAppsSettings(context)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) { Text("ElPiFon als Telefon-App verwenden", fontSize = 17.sp) }
                }
            }
            if (isDialer) {
                item {
                    SwitchRow(
                        title = "Unbekannte Anrufer nicht klingeln lassen",
                        subtitle = "Nummern, die weder in ElPiFon noch im Telefonbuch stehen, erscheinen nur still.",
                        checked = settings.silenceUnknown,
                        onChange = { on -> onSettingsChange { it.copy(silenceUnknown = on) } },
                    )
                }
            }

            item { Section("Startbildschirm") }
            item {
                SwitchRow(
                    title = "ElPiFon als Startbildschirm",
                    subtitle = "Die Home-Taste führt dann immer zu ElPiFon. Danach in der Auswahl „ElPiFon“ antippen.",
                    checked = homeMode,
                    onChange = { on ->
                        PhoneActions.setHomeMode(context, on)
                        homeMode = on
                        PhoneActions.openHomeSettings(context)
                    },
                )
            }
            item {
                OutlinedButton(onClick = { PhoneActions.openHomeSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Anderen Startbildschirm wählen")
                }
            }
            item {
                OutlinedButton(onClick = { PhoneActions.openAndroidSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Android-Einstellungen öffnen")
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun Section(title: String) {
    Column(Modifier.padding(top = 12.dp)) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        HorizontalDivider(Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun ContactRow(
    contact: Contact,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onMove: (Int) -> Unit,
) {
    Card(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ContactAvatar(contact.name, contact.photo, size = 48.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    text = if (contact.emergency) "⚠ ${contact.name}" else contact.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(contact.number, fontSize = 14.sp)
            }
            IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Nach oben")
            }
            IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Nach unten")
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Bearbeiten")
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 18.sp)
            Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Textfeld, das nach einer kurzen Tipp-Pause speichert – und spätestens beim Verlassen. */
@Composable
private fun DebouncedTextField(
    label: String,
    value: String,
    singleLine: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    onCommit: (String) -> Unit,
) {
    var text by remember { mutableStateOf(value) }
    val latestText by rememberUpdatedState(text)
    val latestValue by rememberUpdatedState(value)
    val commit by rememberUpdatedState(onCommit)

    LaunchedEffect(text) {
        if (text != value) {
            delay(600)
            commit(text)
        }
    }
    DisposableEffect(Unit) {
        onDispose { if (latestText != latestValue) commit(latestText) }
    }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (keyboardType == KeyboardType.NumberPassword) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
    )
}
