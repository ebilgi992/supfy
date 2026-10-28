package com.sametozdemir.tini.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Tını paleti: yeşil + siyah (müzik uygulamalarına özgü koyu tema).
val TiniGreen = Color(0xFF1DB954)
val TiniGreenBright = Color(0xFF1ED760)
val TiniBg = Color(0xFF121212)
val TiniSurface = Color(0xFF181818)
val TiniSurfaceHigh = Color(0xFF282828)
val TiniOnSurface = Color(0xFFFFFFFF)
val TiniMuted = Color(0xFFB3B3B3)
val TiniError = Color(0xFFF15E6C)

private val TiniColors = darkColorScheme(
    primary = TiniGreen,
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF0D4A24),
    onPrimaryContainer = Color(0xFFB9F6CA),
    secondary = TiniGreenBright,
    onSecondary = Color(0xFF000000),
    tertiary = TiniGreenBright,
    onTertiary = Color(0xFF000000),
    background = TiniBg,
    onBackground = TiniOnSurface,
    surface = TiniSurface,
    onSurface = TiniOnSurface,
    surfaceVariant = TiniSurfaceHigh,
    onSurfaceVariant = TiniMuted,
    surfaceContainer = TiniSurface,
    surfaceContainerHigh = TiniSurfaceHigh,
    error = TiniError,
    onError = Color(0xFF2A0006),
    errorContainer = Color(0xFF4D1219),
    onErrorContainer = Color(0xFFFFDADC),
    outline = Color(0xFF3E3E3E),
    outlineVariant = Color(0xFF2A2A2A),
)

private val TiniShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

private val base = Typography()
private val TiniTypography = base.copy(
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

@Composable
fun TiniTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TiniColors,
        shapes = TiniShapes,
        typography = TiniTypography,
        content = content,
    )
}
