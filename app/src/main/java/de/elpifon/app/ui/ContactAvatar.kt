package de.elpifon.app.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import de.elpifon.app.data.Photos
import kotlin.math.abs

/** Rundes Foto des Kontakts – oder, ohne Foto, der Anfangsbuchstabe auf farbigem Kreis. */
@Composable
fun ContactAvatar(name: String, photo: String?, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(photo) {
        Photos.file(context, photo)?.let { f -> runCatching { BitmapFactory.decodeFile(f.path)?.asImageBitmap() }.getOrNull() }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape),
        )
    } else {
        val color = Signal.avatars[abs(name.hashCode()) % Signal.avatars.size]
        Box(modifier.size(size).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            Text(
                text = name.trim().take(1).uppercase().ifEmpty { "?" },
                color = Color.White,
                fontSize = (size.value * 0.45f).sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
