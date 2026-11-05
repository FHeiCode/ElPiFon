package de.elpifon.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ElBlue = Color(0xFF1565C0)

/** Hoher Kontrast, klare Farben – gut lesbar auch bei nachlassender Sehkraft. */
private val Light = lightColorScheme(
    primary = ElBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001A41),
    background = Color(0xFFFFFBF5),
    onBackground = Color(0xFF111111),
    surface = Color.White,
    onSurface = Color(0xFF111111),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFAAC7FF),
    onPrimary = Color(0xFF002F65),
    primaryContainer = Color(0xFF0D47A1),
    onPrimaryContainer = Color(0xFFD6E4FF),
    background = Color(0xFF101010),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF1C1C1C),
    onSurface = Color(0xFFF2F2F2),
)

/** Farben mit fester Bedeutung. */
object Signal {
    val emergency = Color(0xFFC62828)
    val onEmergency = Color.White
    val batteryLow = Color(0xFFC62828)
    val batteryOk = Color(0xFF2E7D32)
    val hint = Color(0xFFFFE082)
    val onHint = Color(0xFF111111)

    /** Farbe aller Wähltasten (mit dünnem weißem Rand). */
    val tile = Color(0xFF1565C0)

    /** Hintergrundfarben für Kontakte ohne Foto (Anfangsbuchstabe im Kreis). */
    val avatars = listOf(
        Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFF6A1B9A),
        Color(0xFFEF6C00), Color(0xFF00838F), Color(0xFFAD1457),
    )
}

@Composable
fun ElPiFonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        content = content,
    )
}
