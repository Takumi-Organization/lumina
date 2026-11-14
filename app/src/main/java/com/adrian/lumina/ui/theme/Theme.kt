package com.adrian.lumina.ui.theme

import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adrian.lumina.R
import com.adrian.lumina.domain.model.AccentChoice

/**
 * Dirección artística «Obsidiana / Marfil»: neutros con matiz (nunca grises puros), un acento vibrante y armónicos
 * cálidos, jerarquía tipográfica por contraste de peso y tamaño, esquinas continuas y profundidad por capas.
 */
object Ink {
    // Oscuro: negros profundos con un matiz azul muy leve; el acento brilla sobre ellos.
    val Obsidian = Color(0xFF07070A)
    val Layer1 = Color(0xFF101117)
    val Layer2 = Color(0xFF181A22)
    val Layer3 = Color(0xFF21242E)
    val DarkLine = Color(0xFF262833)
    val Snow = Color(0xFFF5F5F8)
    val Mist = Color(0xFF9EA0AE)
    val Dusk = Color(0xFF6A6D7C)

    // Claro: marfil luminoso, tinta con matiz.
    val Ivory = Color(0xFFF6F4F0)
    val Pearl = Color(0xFFFFFFFF)
    val LightLayer = Color(0xFFEDEAE4)
    val LightLine = Color(0xFFDDD9D1)
    val Ink = Color(0xFF17161C)
    val Slate = Color(0xFF6E6D79)
}

/** Acento principal; los secundarios armónicos (oro, menta, rosa) salen de [Harmony]. */
fun AccentChoice.color(dark: Boolean): Color = when (this) {
    AccentChoice.CORAL -> if (dark) Color(0xFFFF6B4A) else Color(0xFFF0502A)
    AccentChoice.ORO -> if (dark) Color(0xFFFFC247) else Color(0xFFD98A00)
    AccentChoice.MENTA -> if (dark) Color(0xFF3DDC97) else Color(0xFF0FA46A)
    AccentChoice.ROSA -> if (dark) Color(0xFFFF5C8A) else Color(0xFFE02A66)
    AccentChoice.ZAFIRO -> if (dark) Color(0xFF4D8DFF) else Color(0xFF1F63E6)
}

object Harmony {
    val Gold = Color(0xFFFFC247)
    val Mint = Color(0xFF3DDC97)
    val Rose = Color(0xFFFF5C8A)
}

@OptIn(ExperimentalTextApi::class)
private fun inter(weight: Int) = Font(
    R.font.inter_variable, FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.opticalSizing(24.sp)),
)

val InterFamily = FontFamily(inter(300), inter(400), inter(500), inter(600), inter(700), inter(800), inter(900))

private fun scheme(dark: Boolean, accent: Color) = if (dark) darkColorScheme(
    primary = accent, onPrimary = Color(0xFF0A0A0C),
    background = Ink.Obsidian, onBackground = Ink.Snow,
    surface = Ink.Layer1, onSurface = Ink.Snow,
    surfaceVariant = Ink.Layer2, onSurfaceVariant = Ink.Mist,
    outline = Ink.DarkLine, outlineVariant = Ink.DarkLine,
    secondary = Harmony.Gold, onSecondary = Color(0xFF0A0A0C),
    tertiary = Harmony.Mint, onTertiary = Color(0xFF0A0A0C),
    secondaryContainer = Ink.Layer3, onSecondaryContainer = Ink.Snow,
    surfaceContainer = Ink.Layer1, surfaceContainerHigh = Ink.Layer2, surfaceContainerHighest = Ink.Layer3,
    surfaceContainerLow = Ink.Obsidian, surfaceTint = accent,
) else lightColorScheme(
    primary = accent, onPrimary = Color.White,
    background = Ink.Ivory, onBackground = Ink.Ink,
    surface = Ink.Pearl, onSurface = Ink.Ink,
    surfaceVariant = Ink.LightLayer, onSurfaceVariant = Ink.Slate,
    outline = Ink.LightLine, outlineVariant = Ink.LightLine,
    secondary = Color(0xFFD98A00), onSecondary = Color.White,
    tertiary = Color(0xFF0FA46A), onTertiary = Color.White,
    secondaryContainer = Ink.LightLine, onSecondaryContainer = Ink.Ink,
    surfaceContainer = Ink.Pearl, surfaceContainerHigh = Ink.LightLayer, surfaceContainerHighest = Ink.LightLine,
    surfaceContainerLow = Ink.Ivory, surfaceTint = accent,
)

private val Rounded = Shapes(
    extraSmall = RoundedCornerShape(6.dp), small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp), extraLarge = RoundedCornerShape(28.dp),
)

private fun t(size: Int, weight: Int, line: Int = (size * 1.25f).toInt(), tracking: Float = 0f) =
    TextStyle(fontFamily = InterFamily, fontWeight = FontWeight(weight), fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.sp, fontFeatureSettings = "tnum, ss01")

private val LuminaTypography = Typography(
    displayLarge = t(56, 800, 56, -2f),     // mes gigante
    displayMedium = t(40, 800, 42, -1.4f),  // números de gran tamaño
    displaySmall = t(30, 700, 34, -0.8f),
    headlineLarge = t(34, 700, 38, -1f),    // título grande de pantalla
    headlineMedium = t(28, 700, 32, -0.8f),
    headlineSmall = t(22, 700, 26, -0.5f),
    titleLarge = t(20, 650, 24, -0.4f),
    titleMedium = t(16, 600, 22, -0.2f),
    titleSmall = t(14, 600, 20, -0.1f),
    bodyLarge = t(16, 400, 24),
    bodyMedium = t(14, 400, 20),
    bodySmall = t(12, 400, 17),
    labelLarge = t(15, 600, 20),
    labelMedium = t(12, 600, 16, 0.2f),
    labelSmall = t(11, 600, 14, 0.4f),
)

/** Muelles con nombre: toda la interacción de Lumina usa física de muelle, no curvas lineales. */
object Springs {
    fun <T> snappy() = spring<T>(dampingRatio = 0.82f, stiffness = 520f)
    fun <T> smooth() = spring<T>(dampingRatio = 0.9f, stiffness = 260f)
    fun <T> bouncy() = spring<T>(dampingRatio = 0.5f, stiffness = 420f)
    fun <T> gentle() = spring<T>(dampingRatio = 1f, stiffness = 120f)
}

/** Respeta «reducir animaciones»: con el ajuste (o la escala del sistema) apagado, cualquier muelle pasa a ser instantáneo. */
@Composable
fun <T> motion(spec: androidx.compose.animation.core.AnimationSpec<T>): androidx.compose.animation.core.AnimationSpec<T> =
    if (LocalAnimationsEnabled.current) spec else androidx.compose.animation.core.snap()

/** Si es false, las animaciones propias se desactivan (ajuste de usuario o escala de animación del sistema). */
val LocalAnimationsEnabled = staticCompositionLocalOf { true }
val LocalHapticsEnabled = staticCompositionLocalOf { true }
/** Lado en px de las miniaturas de la cuadrícula (ajuste «calidad»). */
val LocalThumbSize = staticCompositionLocalOf { 320 }
val LocalDark = staticCompositionLocalOf { true }

@Composable
fun LuminaTheme(
    dark: Boolean = isSystemInDarkTheme(),
    animations: Boolean = true,
    haptics: Boolean = true,
    thumbPx: Int = 320,
    accent: AccentChoice = AccentChoice.CORAL,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAnimationsEnabled provides animations, LocalHapticsEnabled provides haptics, LocalThumbSize provides thumbPx, LocalDark provides dark,
    ) {
        val colors = scheme(dark, accent.color(dark))
        MaterialTheme(colorScheme = colors, typography = LuminaTypography, shapes = Rounded) {
            androidx.compose.material3.Surface(color = colors.background, contentColor = colors.onBackground, content = content)
        }
    }
}
