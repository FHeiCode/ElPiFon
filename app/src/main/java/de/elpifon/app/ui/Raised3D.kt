package de.elpifon.app.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/** Leichter Schatten unter weißer Schrift auf farbigen Tasten. */
val TextOnColorShadow = Shadow(color = Color.Black.copy(alpha = 0.3f), offset = Offset(0f, 3f), blurRadius = 3f)

/**
 * Taste mit 3D-Effekt: oben heller, unten eine dunkle Kante, darunter ein weicher Schatten.
 * Ist [pressed] true, sinkt die Fläche auf die Kante ein – so sieht man, dass gedrückt wurde.
 */
@Composable
fun Raised3D(
    color: Color,
    pressed: Boolean,
    modifier: Modifier = Modifier,
    corner: Dp = 20.dp,
    depth: Dp = 6.dp,
    borderColor: Color = Color.White.copy(alpha = 0.35f),
    borderWidth: Dp = 1.5.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val sink by animateDpAsState(if (pressed) depth else 0.dp, tween(durationMillis = 70), label = "sink")
    val shape = RoundedCornerShape(corner)
    val edge = lerp(color, Color.Black, 0.4f)
    val top = lerp(color, Color.White, 0.22f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(bottom = depth) // Platz für Kante und Schatten
            .offset { IntOffset(0, sink.roundToPx()) }
            .drawBehind {
                val r = CornerRadius(corner.toPx())
                val rest = (depth - sink).toPx()
                // Weicher Schatten und dunkle Kante unter der Fläche.
                drawRoundRect(Color.Black.copy(alpha = 0.18f), topLeft = Offset(0f, rest + 3.dp.toPx()), size = size, cornerRadius = r)
                if (rest > 0f) drawRoundRect(edge, topLeft = Offset(0f, rest), size = size, cornerRadius = r)
            }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(top, color)))
            .border(borderWidth, borderColor, shape),
        content = content,
    )
}
