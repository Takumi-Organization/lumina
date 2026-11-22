package com.adrian.lumina.ui.theme

import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CutCornerShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adrian.lumina.R
import com.adrian.lumina.domain.model.AccentChoice

/**
 * Identidad Takumi (v1, tokens de `takumi-design/tokens.json`): papel negro y tinta `#F2F2F2`, monocromo estricto
 * (sin acento, degradado ni cristal), radio 0 con la esquina superior derecha cortada en diagonal, Outfit fina y ancha
 * para títulos y JetBrains Mono para datos y etiquetas. Seleccionado = inversión (fondo tinta, texto fondo).
 */
object Ink {
    // Oscuro (por defecto)
    val Obsidian = Color(0xFF000000)   // fondo
    val Layer1 = Color(0xFF080808)     // panel
    val Layer2 = Color(0xFF111111)     // elevado
    val Layer3 = Color(0xFF1C1C1C)     // línea (también peldaño sobre «elevado»)
    val DarkLine = Color(0xFF3A3A3A)   // línea alta: bordes de tarjetas y controles
    val DarkHair = Color(0xFF1C1C1C)   // línea: separadores de filas
    val Snow = Color(0xFFF2F2F2)       // tinta
    val Mist = Color(0xFF8C8C8C)       // tinta apagada
    val Dusk = Color(0xFF4A4A4A)       // tenue: solo decorativo

    // Claro («papel»)
    val Ivory = Color(0xFFF4F4F4)
    val Pearl = Color(0xFFFFFFFF)
    val LightLayer = Color(0xFFEBEBEB)
    val LightLine = Color(0xFFC0C0C0)
    val LightHair = Color(0xFFE0E0E0)
    val Ink = Color(0xFF0A0A0A)
    val Slate = Color(0xFF6B6B6B)
}

/**
 * Takumi no tiene acento: cualquier valor guardado de `AccentChoice` (se conserva el enum para no romper DataStore)
 * resuelve a la tinta. El ajuste ya no se muestra.
 */
@Suppress("UnusedReceiverParameter")
fun AccentChoice.color(dark: Boolean): Color = if (dark) Ink.Snow else Ink.Ink

@Suppress("UnusedReceiverParameter")
fun AccentChoice.onColor(dark: Boolean): Color = if (dark) Color.Black else Ink.Ivory

/** Sin color de apoyo: lo secundario se resuelve con luminancia. */
object Harmony {
    val Gold = Ink.Mist
    val Mint = Ink.Mist
    /** Favorito: la tinta (el corazón se distingue por relleno, no por color). */
    val Rose = Ink.Snow
}

@OptIn(ExperimentalTextApi::class)
private fun outfit(weight: Int) = Font(
    R.font.outfit_variable, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val OutfitFamily = FontFamily(outfit(200), outfit(300), outfit(400), outfit(500), outfit(600))
val MonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal), Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
)

private fun scheme(dark: Boolean) = if (dark) darkColorScheme(
    primary = Ink.Snow, onPrimary = Color.Black,
    background = Ink.Obsidian, onBackground = Ink.Snow,
    surface = Ink.Layer1, onSurface = Ink.Snow,
    surfaceVariant = Ink.Layer2, onSurfaceVariant = Ink.Mist,
    outline = Ink.DarkLine, outlineVariant = Ink.DarkHair,
    secondary = Color(0xFFC9C9C9), onSecondary = Color.Black,
    tertiary = Ink.Mist, onTertiary = Color.Black,
    error = Ink.Snow, onError = Color.Black,
    secondaryContainer = Ink.Layer3, onSecondaryContainer = Ink.Snow,
    surfaceContainer = Ink.Layer1, surfaceContainerHigh = Ink.Layer2, surfaceContainerHighest = Ink.Layer3,
    surfaceContainerLow = Ink.Obsidian, surfaceTint = Color.Transparent,
) else lightColorScheme(
    primary = Ink.Ink, onPrimary = Ink.Ivory,
    background = Ink.Ivory, onBackground = Ink.Ink,
    surface = Ink.Pearl, onSurface = Ink.Ink,
    surfaceVariant = Ink.LightLayer, onSurfaceVariant = Ink.Slate,
    outline = Ink.LightLine, outlineVariant = Ink.LightHair,
    secondary = Color(0xFF333333), onSecondary = Ink.Ivory,
    tertiary = Ink.Slate, onTertiary = Ink.Ivory,
    error = Ink.Ink, onError = Ink.Ivory,
    secondaryContainer = Ink.LightHair, onSecondaryContainer = Ink.Ink,
    surfaceContainer = Ink.Pearl, surfaceContainerHigh = Ink.LightLayer, surfaceContainerHighest = Ink.LightHair,
    surfaceContainerLow = Ink.Ivory, surfaceTint = Color.Transparent,
)

/** Corte diagonal del logo en la esquina superior derecha (máx. 22 dp, el «lg» de Takumi); el resto, ángulo recto. */
fun TakumiCut(cut: Dp) = CutCornerShape(topStart = 0.dp, topEnd = cut.coerceAtMost(22.dp), bottomEnd = 0.dp, bottomStart = 0.dp)
val TakumiSm = TakumiCut(10.dp)
val TakumiMd = TakumiCut(14.dp)
val TakumiLg = TakumiCut(22.dp)

private val TakumiShapes = Shapes(
    extraSmall = TakumiCut(6.dp), small = TakumiSm, medium = TakumiMd, large = TakumiLg, extraLarge = TakumiLg,
)

private fun ui(size: Int, weight: Int, line: Int = (size * 1.35f).toInt(), tracking: Float = 0f) =
    TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight(weight), fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.sp)

private fun mono(size: Int, weight: Int = 400, line: Int = (size * 1.5f).toInt(), tracking: Float = 0f) =
    TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight(weight), fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.sp, fontFeatureSettings = "tnum")

private val LuminaTypography = Typography(
    displayLarge = ui(52, 200, 58, 2f),      // mes gigante
    displayMedium = mono(34, 400, 40),       // cifras grandes (datos en mono)
    displaySmall = ui(30, 200, 36, 1f),
    headlineLarge = ui(30, 200, 38, 4f),     // título de pantalla: fina, ancha
    headlineMedium = ui(26, 200, 32, 3f),
    headlineSmall = ui(22, 200, 28, 2f),
    titleLarge = ui(20, 300, 26, 1f),
    titleMedium = ui(16, 400, 22, 0.5f),
    titleSmall = ui(14, 500, 20, 0.5f),
    bodyLarge = ui(16, 300, 24),
    bodyMedium = ui(14, 300, 20),
    bodySmall = ui(13, 300, 18),
    labelLarge = mono(13, 500, 18, 1.2f),    // botones y píldoras: mono espaciada
    labelMedium = mono(12, 400, 16, 1.4f),
    labelSmall = mono(11, 400, 14, 1.2f),
)

/** Muelles con nombre (Takumi pide movimiento sin rebotes: todos críticamente amortiguados). */
object Springs {
    fun <T> snappy() = spring<T>(dampingRatio = 1f, stiffness = 700f)
    fun <T> smooth() = spring<T>(dampingRatio = 1f, stiffness = 320f)
    fun <T> bouncy() = spring<T>(dampingRatio = 1f, stiffness = 520f)
    fun <T> gentle() = spring<T>(dampingRatio = 1f, stiffness = 140f)
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
    @Suppress("UNUSED_PARAMETER") accent: AccentChoice = AccentChoice.ORO,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAnimationsEnabled provides animations, LocalHapticsEnabled provides haptics, LocalThumbSize provides thumbPx, LocalDark provides dark,
    ) {
        val colors = scheme(dark)
        MaterialTheme(colorScheme = colors, typography = LuminaTypography, shapes = TakumiShapes) {
            androidx.compose.material3.Surface(color = colors.background, contentColor = colors.onBackground, content = content)
        }
    }
}
