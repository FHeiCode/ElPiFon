package de.elpifon.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.elpifon.app.data.AppSettings
import de.elpifon.app.data.Contact
import de.elpifon.app.data.ContactStore
import de.elpifon.app.data.Photos
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Welche Ansicht gerade zu sehen ist. */
sealed interface Screen {
    data object Home : Screen
    data object Settings : Screen
    /** [contactId] = null legt einen neuen Kontakt an. */
    data class EditContact(val contactId: String?) : Screen
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ContactStore(app)

    /** null, solange noch geladen wird (verhindert kurzes Aufblitzen von „keine Kontakte“). */
    val contacts: StateFlow<List<Contact>?> = store.contacts.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val settings: StateFlow<AppSettings> = store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    var screen by mutableStateOf<Screen>(Screen.Home)
        private set

    fun show(target: Screen) {
        screen = target
    }

    fun save(contact: Contact, oldPhoto: String?) = viewModelScope.launch {
        store.upsert(contact)
        if (oldPhoto != null && oldPhoto != contact.photo) Photos.delete(getApplication(), oldPhoto)
    }

    fun delete(contact: Contact) = viewModelScope.launch {
        store.remove(contact)
        Photos.delete(getApplication(), contact.photo)
    }

    fun move(contact: Contact, delta: Int) = viewModelScope.launch { store.move(contact, delta) }

    fun updateSettings(transform: (AppSettings) -> AppSettings) = viewModelScope.launch {
        store.updateSettings(transform)
    }
}
