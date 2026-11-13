package com.adrian.lumina.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** «Cámara oscura»: tinta cálida, una sola luz ámbar. Ver docs/ARQUITECTURA.md §3. */
object LuminaColors {
    val Ink = Color(0xFF0E0D0C)
    val InkRaised = Color(0xFF1A1816)
    val InkLine = Color(0xFF2B2824)
    val Bone = Color(0xFFEDE8DF)
    val BoneDim = Color(0xFF9A9388)
    val Lumen = Color(0xFFE8A33D)

    val Paper = Color(0xFFF6F2EA)
    val PaperRaised = Color(0xFFEAE4D8)
    val PaperLine = Color(0xFFD6CFC0)
    val Charcoal = Color(0xFF1B1916)
    val CharcoalDim = Color(0xFF6B655B)
    val LumenDeep = Color(0xFFB86E00)
}

private val Dark = darkColorScheme(
    primary = LuminaColors.Lumen, onPrimary = LuminaColors.Ink,
    background = LuminaColors.Ink, onBackground = LuminaColors.Bone,
    surface = LuminaColors.Ink, onSurface = LuminaColors.Bone,
    surfaceVariant = LuminaColors.InkRaised, onSurfaceVariant = LuminaColors.BoneDim,
    outline = LuminaColors.InkLine, outlineVariant = LuminaColors.InkLine,
)
private val Light = lightColorScheme(
    primary = LuminaColors.LumenDeep, onPrimary = LuminaColors.Paper,
    background = LuminaColors.Paper, onBackground = LuminaColors.Charcoal,
    surface = LuminaColors.Paper, onSurface = LuminaColors.Charcoal,
    surfaceVariant = LuminaColors.PaperRaised, onSurfaceVariant = LuminaColors.CharcoalDim,
    outline = LuminaColors.PaperLine, outlineVariant = LuminaColors.PaperLine,
)

private val LuminaTypography = Typography(
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light, fontSize = 28.sp, letterSpacing = (-0.5).sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 17.sp, letterSpacing = (-0.2).sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.4.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp),
)

/** Si es false, las animaciones propias se desactivan (ajuste de usuario; ver Fase 8). */
val LocalAnimationsEnabled = staticCompositionLocalOf { true }

@Composable
fun LuminaTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = LuminaTypography, content = content)
}
