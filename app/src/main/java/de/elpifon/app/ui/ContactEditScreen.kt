package de.elpifon.app.ui

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.elpifon.app.data.Contact
import de.elpifon.app.data.Photos
import kotlinx.coroutines.launch

/** Kontakt anlegen oder bearbeiten. [existing] = null legt einen neuen an. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactEditScreen(
    existing: Contact?,
    onSave: (contact: Contact, oldPhoto: String?) -> Unit,
    onDelete: (Contact) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var number by remember { mutableStateOf(existing?.number ?: "") }
    var photo by remember { mutableStateOf(existing?.photo) }
    var emergency by remember { mutableStateOf(existing?.emergency ?: false) }
    var busy by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    /** Neu gewählte, noch nicht gespeicherte Fotos – beim Abbrechen wieder löschen. */
    val unsavedPhotos = remember { mutableListOf<String>() }

    fun importPhoto(uri: Uri) {
        busy = true
        scope.launch {
            Photos.save(context, uri)?.let {
                unsavedPhotos += it
                photo = it
            }
            busy = false
        }
    }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(::importPhoto)
    }

    // Aus dem Telefonbuch: Name, Nummer und – falls vorhanden – das Kontaktfoto übernehmen.
    val pickContact = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                ),
                null, null, null,
            )?.use { c ->
                if (c.moveToFirst()) {
                    c.getString(0)?.takeIf { it.isNotBlank() }?.let { name = it }
                    c.getString(1)?.takeIf { it.isNotBlank() }?.let { number = it }
                    if (photo == null) c.getString(2)?.let { importPhoto(Uri.parse(it)) }
                }
            }
        }
    }

    fun cancel() {
        unsavedPhotos.forEach { Photos.delete(context, it) }
        onCancel()
    }

    BackHandler { cancel() }

    val canSave = name.isNotBlank() && number.any { it.isDigit() } && !busy

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Neuer Kontakt" else "Kontakt bearbeiten") },
                navigationIcon = {
                    IconButton(onClick = ::cancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Abbrechen")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedButton(
                onClick = { pickContact.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Contacts, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Aus dem Telefonbuch übernehmen")
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name auf der Kachel (z. B. „Tochter Anna“)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = number,
                onValueChange = { number = it },
                label = { Text("Telefonnummer") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                ContactAvatar(name.ifBlank { "?" }, photo, size = 96.dp)
                Spacer(Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { pickPhoto.launch("image/*") }, enabled = !busy) {
                        Icon(Icons.Filled.Photo, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (busy) "Wird geladen …" else "Foto wählen")
                    }
                    if (photo != null) {
                        TextButton(onClick = { photo = null }) { Text("Foto entfernen") }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = emergency, onCheckedChange = { emergency = it })
                Column {
                    Text("Notfallkontakt", fontSize = 18.sp)
                    Text(
                        "Bekommt beim Notruf eine SMS (mit Standort).",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }

            Button(
                onClick = {
                    val contact = (existing ?: Contact(name = "", number = "")).copy(
                        name = name.trim(),
                        number = number.trim(),
                        photo = photo,
                        emergency = emergency,
                    )
                    // Zwischendurch gewählte, aber nicht genommene Fotos aufräumen.
                    unsavedPhotos.filter { it != photo }.forEach { Photos.delete(context, it) }
                    onSave(contact, existing?.photo)
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text("Speichern", fontSize = 18.sp)
            }

            if (existing != null) {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Signal.emergency),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Kontakt löschen")
                }
            }
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Kontakt löschen?") },
            text = { Text("„${existing.name}“ wird vom Startbildschirm entfernt.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    unsavedPhotos.forEach { Photos.delete(context, it) }
                    onDelete(existing)
                }) { Text("Löschen") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Behalten") } },
        )
    }
}
