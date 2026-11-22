package com.adrian.lumina.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Takumi no usa cristal ni desenfoque: las capas flotantes (barras, hojas, burbujas) son paneles opacos con borde de
 * 1 dp. Se conservan los nombres de la API anterior (`glass`, `glassBar`, `glassSource`) para no tocar cada pantalla.
 */
class GlassBackdrop internal constructor()

@Composable
fun rememberGlassBackdrop(): GlassBackdrop = androidx.compose.runtime.remember { GlassBackdrop() }

val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }

/** Alto que ocupa la barra de pestañas flotante (más la barra de navegación): el contenido lo suma a su relleno inferior. */
val LocalBottomInset = staticCompositionLocalOf { 0.dp }

/** Ya no graba nada: sin desenfoque no hay fondo que capturar. */
@Suppress("UnusedReceiverParameter", "UNUSED_PARAMETER")
fun Modifier.glassSource(backdrop: GlassBackdrop): Modifier = this

/** Panel opaco (fondo `panel` o [tint]) con borde `lineaAlta`. [blur] se ignora. */
@Composable
@Suppress("UNUSED_PARAMETER")
fun Modifier.glass(shape: Shape, blur: Dp = 0.dp, tint: Color? = null, outline: Boolean = true): Modifier {
    val fill = tint ?: MaterialTheme.colorScheme.surface
    val line = MaterialTheme.colorScheme.outline
    return this.clip(shape).background(fill).then(if (outline) Modifier.border(1.dp, line, shape) else Modifier)
}

/** Barra a todo el ancho: panel opaco con un filete en el borde que da al contenido. */
@Composable
fun Modifier.glassBar(atBottom: Boolean = true): Modifier {
    val line = MaterialTheme.colorScheme.outline
    return this.glass(RectangleShape, outline = false).drawBehind {
        val h = 1.dp.toPx()
        drawRect(line, topLeft = Offset(0f, if (atBottom) size.height - h else 0f), size = androidx.compose.ui.geometry.Size(size.width, h))
    }
}
