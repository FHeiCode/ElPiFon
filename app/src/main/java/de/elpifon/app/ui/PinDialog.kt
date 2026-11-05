package de.elpifon.app.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation

/** Fragt die PIN ab, bevor die Einstellungen geöffnet werden. */
@Composable
fun PinDialog(expectedPin: String, onSuccess: () -> Unit, onDismiss: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }

    fun check() {
        if (input == expectedPin) onSuccess() else {
            wrong = true
            input = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PIN eingeben") },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { v ->
                    input = v.filter(Char::isDigit)
                    wrong = false
                    if (input.length == expectedPin.length && input == expectedPin) onSuccess()
                },
                label = { Text(if (wrong) "Falsche PIN" else "PIN") },
                isError = wrong,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            )
        },
        confirmButton = { TextButton(onClick = ::check) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
