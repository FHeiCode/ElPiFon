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
    /** Notfallkontakte werden beim Notruf der Reihe nach angerufen. */
    val emergency: Boolean = false,
    /** Notfallkontakt bekommt beim Notruf zusätzlich eine SMS (bei Festnetz abschalten). */
    val emergencySms: Boolean = true,
)

/** Deutsche Handynummern beginnen mit 015, 016 oder 017 (auch +49 / 0049). */
fun looksLikeMobile(number: String): Boolean {
    val digits = number.filter { it.isDigit() }
    val national = when {
        number.trim().startsWith("+49") -> "0" + digits.removePrefix("49")
        digits.startsWith("0049") -> "0" + digits.removePrefix("0049")
        number.trim().startsWith("+") || digits.startsWith("00") -> return true // Ausland: nicht raten
        else -> digits
    }
    return national.startsWith("015") || national.startsWith("016") || national.startsWith("017")
}

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
