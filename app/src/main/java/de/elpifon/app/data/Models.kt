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
    /** Notruf-Kette: so lange klingelt es pro Kontakt, bevor der Nächste angerufen wird. */
    val emergencyWaitSeconds: Int = DEFAULT_WAIT_SECONDS,
    /** Notruf-Kette: so oft wird die ganze Liste der Notfallkontakte angerufen. */
    val emergencyRounds: Int = DEFAULT_ROUNDS,
) {
    companion object {
        const val DEFAULT_WAIT_SECONDS = 20
        const val DEFAULT_ROUNDS = 3
        val WAIT_RANGE = 10..60
        val ROUNDS_RANGE = 1..10
        const val DEFAULT_EMERGENCY_TEXT = "Ich brauche Hilfe! Bitte ruf mich sofort zurück."
    }
}
