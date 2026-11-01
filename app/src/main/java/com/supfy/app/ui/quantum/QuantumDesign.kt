package com.supfy.app.ui.quantum

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal val QuantumInk = Color(0xFF0B1010)
internal val QuantumPanel = Color(0xFF182121)
internal val QuantumGreen = Color(0xFF83F3AE)
internal val QuantumWhite = Color(0xFFF4FAF6)
internal val QuantumMuted = Color(0xFFB2C2BA)

/** Decorative brand graphic, deliberately never presented as album artwork. */
@Composable
internal fun QuantumRecord(modifier: Modifier = Modifier, accent: Color = QuantumGreen, compact: Boolean = false) {
    Box(
        modifier = modifier.aspectRatio(1f).clip(RoundedCornerShape(if (compact) 12.dp else 24.dp))
            .background(Brush.linearGradient(listOf(accent.copy(alpha = .24f), QuantumPanel, QuantumInk)))
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(if (compact) 5.dp else 22.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension * .41f
            for (ring in 1..4) {
                drawCircle(accent.copy(alpha = .08f + ring * .025f), radius * ring / 4f, center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
            }
            val heights = listOf(.27f, .48f, .72f, .41f, .58f)
            val gap = radius * .25f
            heights.forEachIndexed { index, height ->
                val x = center.x + (index - 2) * gap
                drawLine(accent, Offset(x, center.y - radius * height / 2),
                    Offset(x, center.y + radius * height / 2), strokeWidth = radius * .09f,
                    cap = StrokeCap.Round)
            }
        }
        if (!compact) {
            Text("SUPFY", modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
                color = QuantumWhite.copy(alpha = .72f), style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold)
        }
    }
}

internal fun quantumAccent(title: String): Color {
    val colors = listOf(QuantumGreen, Color(0xFF79D8CF), Color(0xFFB5D59B), Color(0xFFA4C5E4))
    val hash = title.take(200).fold(0) { value, character -> 31 * value + character.code }
    return colors[(hash and Int.MAX_VALUE) % colors.size]
}

internal object QuantumIcons {
    val Library = glyph("Library") {
        moveTo(4f, 4f); lineTo(4f, 20f); moveTo(9f, 4f); lineTo(9f, 20f)
        moveTo(14f, 5f); lineTo(18f, 4f); lineTo(22f, 19f); lineTo(18f, 20f); close()
    }
    val Pause = glyph("Pause") {
        moveTo(8f, 5f); lineTo(8f, 19f); moveTo(16f, 5f); lineTo(16f, 19f)
    }
    val Previous = glyph("Previous") {
        moveTo(5f, 5f); lineTo(5f, 19f)
        moveTo(19f, 5f); lineTo(8f, 12f); lineTo(19f, 19f); close()
    }
    val Next = glyph("Next") {
        moveTo(19f, 5f); lineTo(19f, 19f)
        moveTo(5f, 5f); lineTo(16f, 12f); lineTo(5f, 19f); close()
    }
    val Queue = glyph("Queue") {
        moveTo(4f, 6f); lineTo(20f, 6f); moveTo(4f, 11f); lineTo(15f, 11f)
        moveTo(4f, 16f); lineTo(12f, 16f)
        moveTo(18f, 14f); lineTo(22f, 17f); lineTo(18f, 20f); close()
    }
}

private fun glyph(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = block)
    }.build()
