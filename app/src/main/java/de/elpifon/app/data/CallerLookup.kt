package de.elpifon.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/** Findet zu einer Telefonnummer den passenden Namen (und bei ElPiFon-Kontakten das Foto). */
object CallerLookup {

    /** Vergleicht nur die letzten 9 Ziffern – so passen „+49 170 …“ und „0170 …“ zusammen. */
    private fun key(number: String): String = number.filter { it.isDigit() }.takeLast(9)

    fun sameNumber(a: String, b: String): Boolean {
        val ka = key(a)
        val kb = key(b)
        return ka.length >= 6 && ka == kb
    }

    fun findContact(contacts: List<Contact>, number: String): Contact? =
        if (number.isBlank()) null else contacts.firstOrNull { sameNumber(it.number, number) }

    /** Name aus dem Android-Telefonbuch (nur mit Kontakt-Berechtigung), sonst null. */
    fun phonebookName(context: Context, number: String): String? {
        if (number.isBlank()) return null
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return null
        return runCatching {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
    }

    /** Ist der Anrufer bekannt (ElPiFon-Kontakt oder Telefonbuch)? */
    fun isKnown(context: Context, contacts: List<Contact>, number: String): Boolean =
        findContact(contacts, number) != null || phonebookName(context, number) != null
}
