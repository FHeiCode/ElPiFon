package de.elpifon.app.data

import kotlinx.serialization.Serializable
import java.util.UUID

/** Ein Kontakt als große Kachel auf dem Startbildschirm. */
@Serializable
data class Contact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val number: String,
    /** Dateiname des Fotos im internen App-Speicher (Ordner "fotos"), oder null. */
    val photo: String? = null,
    /** Notfallkontakte bekommen beim Notruf eine SMS; der erste wird angerufen. */
    val emergency: Boolean = false,
)

/** Einstellungen, die Angehörige festlegen. */
@Serializable
data class AppSettings(
    val emergencyText: String = DEFAULT_EMERGENCY_TEXT,
    val sendLocation: Boolean = true,
    /** Leer = keine PIN für die Einstellungen. */
    val pin: String = "",
    /** Unbekannte Nummern klingeln nicht (Anruf erscheint trotzdem). Nur als Telefon-App. */
    val silenceUnknown: Boolean = false,
) {
    companion object {
        const val DEFAULT_EMERGENCY_TEXT = "Ich brauche Hilfe! Bitte ruf mich sofort zurück."
    }
}
