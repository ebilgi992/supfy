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

// Supfy: ferah yeşil, kömür ve düşük yoğunluklu teal yüzeyler.
val SupfyGreen = Color(0xFF83F3AE)
val SupfyGreenBright = Color(0xFFA2F8C2)
val SupfyBg = Color(0xFF0B1010)
val SupfySurface = Color(0xFF141C1B)
val SupfySurfaceHigh = Color(0xFF202D29)
val SupfyOnSurface = Color(0xFFF4FAF6)
val SupfyMuted = Color(0xFFB2C2BA)
val SupfyError = Color(0xFFF15E6C)

private val SupfyDarkColors = darkColorScheme(
    primary = SupfyGreen,
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF173C2B),
    onPrimaryContainer = Color(0xFFC5F9D7),
    secondary = SupfyGreenBright,
    onSecondary = Color(0xFF000000),
    tertiary = Color(0xFF79D8CF),
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
    outline = Color(0xFF52665B),
    outlineVariant = Color(0xFF2E3C35),
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
