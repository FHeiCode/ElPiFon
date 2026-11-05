package de.elpifon.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.elpifon.app.call.CallActivity
import de.elpifon.app.call.CallManager
import de.elpifon.app.data.PhoneActions
import de.elpifon.app.ui.ContactEditScreen
import de.elpifon.app.ui.ElPiFonTheme
import de.elpifon.app.ui.HomeScreen
import de.elpifon.app.ui.MainViewModel
import de.elpifon.app.ui.PinDialog
import de.elpifon.app.ui.Screen
import de.elpifon.app.ui.SettingsScreen

/** Berechtigungen, die beim Start erfragt werden (jeweils einmal, Android merkt sich die Antwort). */
private val PERMISSIONS = arrayOf(
    Manifest.permission.CALL_PHONE,
    Manifest.permission.SEND_SMS,
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.READ_CONTACTS,
)

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ElPiFonTheme {
                val context = LocalContext.current
                val contacts by viewModel.contacts.collectAsStateWithLifecycle()
                val settings by viewModel.settings.collectAsStateWithLifecycle()
                var askPin by remember { mutableStateOf(false) }
                val ongoingCall by CallManager.current.collectAsStateWithLifecycle()

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { /* Ergebnis wird beim Anrufen/Notruf jeweils neu geprüft. */ }
                LaunchedEffect(Unit) {
                    val missing = PERMISSIONS.filter {
                        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                    }
                    if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
                }

                when (val screen = viewModel.screen) {
                    Screen.Home -> {
                        // Im Startbildschirm-Modus darf „Zurück“ die App nicht verlassen.
                        BackHandler(enabled = PhoneActions.isHomeModeEnabled(context)) { }
                        HomeScreen(
                            contacts = contacts,
                            onCall = { PhoneActions.call(context, it.number) },
                            onEmergency = {
                                PhoneActions.triggerEmergency(context, contacts.orEmpty(), settings)
                            },
                            onOpenSettings = {
                                if (settings.pin.isEmpty()) viewModel.show(Screen.Settings) else askPin = true
                            },
                            ongoingCall = ongoingCall != null,
                            onReturnToCall = {
                                context.startActivity(android.content.Intent(context, CallActivity::class.java))
                            },
                        )
                        if (askPin) {
                            PinDialog(
                                expectedPin = settings.pin,
                                onSuccess = {
                                    askPin = false
                                    viewModel.show(Screen.Settings)
                                },
                                onDismiss = { askPin = false },
                            )
                        }
                    }

                    Screen.Settings -> {
                        BackHandler { viewModel.show(Screen.Home) }
                        SettingsScreen(
                            contacts = contacts.orEmpty(),
                            settings = settings,
                            onBack = { viewModel.show(Screen.Home) },
                            onAdd = { viewModel.show(Screen.EditContact(null)) },
                            onEdit = { viewModel.show(Screen.EditContact(it.id)) },
                            onMove = { contact, delta -> viewModel.move(contact, delta) },
                            onSettingsChange = { transform -> viewModel.updateSettings(transform) },
                        )
                    }

                    is Screen.EditContact -> {
                        val existing = screen.contactId?.let { id -> contacts?.firstOrNull { it.id == id } }
                        ContactEditScreen(
                            existing = existing,
                            onSave = { contact, oldPhoto ->
                                viewModel.save(contact, oldPhoto)
                                viewModel.show(Screen.Settings)
                            },
                            onDelete = { contact ->
                                viewModel.delete(contact)
                                viewModel.show(Screen.Settings)
                            },
                            onCancel = { viewModel.show(Screen.Settings) },
                        )
                    }
                }
            }
        }
    }

    /** Home-Taste im Startbildschirm-Modus: immer zurück zur Kachel-Ansicht. */
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(android.content.Intent.CATEGORY_HOME)) viewModel.show(Screen.Home)
    }
}
