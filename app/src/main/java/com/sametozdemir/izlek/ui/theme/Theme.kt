package com.sametozdemir.izlek.ui.theme

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

// İzlek paleti: mercan + menekşe, koyu zemin. Spotify'ın yeşil/siyah kimliğinden bilinçli olarak ayrıdır.
val IzlekCoral = Color(0xFFFF7A59)
val IzlekViolet = Color(0xFF8B7CF6)
val IzlekTeal = Color(0xFF5EEAD4)
val IzlekBg = Color(0xFF0F1115)
val IzlekSurface = Color(0xFF171A21)
val IzlekSurfaceHigh = Color(0xFF222633)
val IzlekOnSurface = Color(0xFFECEDEF)
val IzlekMuted = Color(0xFFA7ABB7)
val IzlekError = Color(0xFFFF5C6C)

private val IzlekColors = darkColorScheme(
    primary = IzlekCoral,
    onPrimary = Color(0xFF1A0E09),
    primaryContainer = Color(0xFF4A2418),
    onPrimaryContainer = Color(0xFFFFDBD0),
    secondary = IzlekViolet,
    onSecondary = Color(0xFF130E2E),
    tertiary = IzlekTeal,
    onTertiary = Color(0xFF00201C),
    background = IzlekBg,
    onBackground = IzlekOnSurface,
    surface = IzlekSurface,
    onSurface = IzlekOnSurface,
    surfaceVariant = IzlekSurfaceHigh,
    onSurfaceVariant = IzlekMuted,
    surfaceContainer = IzlekSurface,
    surfaceContainerHigh = IzlekSurfaceHigh,
    error = IzlekError,
    onError = Color(0xFF2A0006),
    errorContainer = Color(0xFF4D1219),
    onErrorContainer = Color(0xFFFFDADC),
    outline = Color(0xFF3A3F4E),
    outlineVariant = Color(0xFF2A2E3A),
)

private val IzlekShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

private val base = Typography()
private val IzlekTypography = base.copy(
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

@Composable
fun IzlekTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = IzlekColors,
        shapes = IzlekShapes,
        typography = IzlekTypography,
        content = content,
    )
}
