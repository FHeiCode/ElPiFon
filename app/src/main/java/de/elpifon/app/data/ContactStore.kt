package de.elpifon.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "elpifon")

/** Speichert Kontakte und Einstellungen dauerhaft auf dem Gerät. */
class ContactStore(private val context: Context) {
    private val contactsKey = stringPreferencesKey("contacts")
    private val settingsKey = stringPreferencesKey("settings")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val contacts: Flow<List<Contact>> = context.dataStore.data.map { prefs -> decodeContacts(prefs[contactsKey]) }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs -> decodeSettings(prefs[settingsKey]) }

    /** Neuer Kontakt kommt ans Ende; ein vorhandener (gleiche id) wird an seiner Stelle ersetzt. */
    suspend fun upsert(contact: Contact) = updateContacts { list ->
        if (list.any { it.id == contact.id }) list.map { if (it.id == contact.id) contact else it }
        else list + contact
    }

    suspend fun remove(contact: Contact) = updateContacts { list -> list.filterNot { it.id == contact.id } }

    /** Verschiebt einen Kontakt um [delta] Plätze (−1 = nach oben, +1 = nach unten). */
    suspend fun move(contact: Contact, delta: Int) = updateContacts { list ->
        val from = list.indexOfFirst { it.id == contact.id }
        val to = from + delta
        if (from < 0 || to !in list.indices) list
        else list.toMutableList().apply { add(to, removeAt(from)) }
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[settingsKey] = json.encodeToString(transform(decodeSettings(prefs[settingsKey])))
        }
    }

    private suspend fun updateContacts(transform: (List<Contact>) -> List<Contact>) {
        context.dataStore.edit { prefs ->
            prefs[contactsKey] = json.encodeToString(transform(decodeContacts(prefs[contactsKey])))
        }
    }

    private fun decodeContacts(raw: String?): List<Contact> =
        raw?.let { runCatching { json.decodeFromString<List<Contact>>(it) }.getOrNull() } ?: emptyList()

    private fun decodeSettings(raw: String?): AppSettings =
        raw?.let { runCatching { json.decodeFromString<AppSettings>(it) }.getOrNull() } ?: AppSettings()
}
