package com.supfy.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Supfy paleti: yeşil + siyah (müzik uygulamalarına özgü koyu tema).
val SupfyGreen = Color(0xFF1DB954)
val SupfyGreenBright = Color(0xFF1ED760)
val SupfyBg = Color(0xFF121212)
val SupfySurface = Color(0xFF181818)
val SupfySurfaceHigh = Color(0xFF282828)
val SupfyOnSurface = Color(0xFFFFFFFF)
val SupfyMuted = Color(0xFFB3B3B3)
val SupfyError = Color(0xFFF15E6C)

private val SupfyDarkColors = darkColorScheme(
    primary = SupfyGreen,
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF0D4A24),
    onPrimaryContainer = Color(0xFFB9F6CA),
    secondary = SupfyGreenBright,
    onSecondary = Color(0xFF000000),
    tertiary = SupfyGreenBright,
    onTertiary = Color(0xFF000000),
    background = SupfyBg,
    onBackground = SupfyOnSurface,
    surface = SupfySurface,
    onSurface = SupfyOnSurface,
    surfaceVariant = SupfySurfaceHigh,
    onSurfaceVariant = SupfyMuted,
    surfaceContainer = SupfySurface,
    surfaceContainerHigh = SupfySurfaceHigh,
    error = SupfyError,
    onError = Color(0xFF2A0006),
    errorContainer = Color(0xFF4D1219),
    onErrorContainer = Color(0xFFFFDADC),
    outline = Color(0xFF3E3E3E),
    outlineVariant = Color(0xFF2A2A2A),
)

// Açık tema: beyaz zeminde okunabilirlik için yeşil biraz koyulaştırıldı (WCAG kontrastı).
private val SupfyLightColors = lightColorScheme(
    primary = Color(0xFF14833B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC8F5D6),
    onPrimaryContainer = Color(0xFF00210B),
    secondary = Color(0xFF14833B),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF14833B),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF121212),
    surface = Color(0xFFF4F4F4),
    onSurface = Color(0xFF121212),
    surfaceVariant = Color(0xFFE8E8E8),
    onSurfaceVariant = Color(0xFF555555),
    surfaceContainer = Color(0xFFF4F4F4),
    surfaceContainerHigh = Color(0xFFE8E8E8),
    error = Color(0xFFC62839),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDADC),
    onErrorContainer = Color(0xFF410008),
    outline = Color(0xFFCFCFCF),
    outlineVariant = Color(0xFFE2E2E2),
)

private val SupfyShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

private val base = Typography()
private val SupfyTypography = base.copy(
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

@Composable
fun SupfyTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) SupfyDarkColors else SupfyLightColors,
        shapes = SupfyShapes,
        typography = SupfyTypography,
        content = content,
    )
}
