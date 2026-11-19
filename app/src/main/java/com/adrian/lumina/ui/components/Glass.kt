package com.adrian.lumina.ui.components

import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.adrian.lumina.ui.theme.LocalDark

/**
 * Cristal de la identidad Ryoku Glass.
 *
 * Android no tiene «backdrop-filter»: se imita grabando el contenido que queda DETRÁS en una capa gráfica
 * ([glassSource]) y dibujando esa capa desenfocada bajo cada superficie flotante ([glass]). Solo funciona si la superficie
 * flotante es hermana (no hija) del contenido grabado. Con API < 31 (sin `RenderEffect`) o sin fondo registrado, la
 * superficie pasa a opaca con el mismo diseño, igual que con «reducir transparencia» en la web.
 */
class GlassBackdrop internal constructor(internal val layer: GraphicsLayer) {
    /** Posición en pantalla del contenido grabado; el cristal la usa para alinear el fondo desenfocado. */
    internal var origin by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberGlassBackdrop(): GlassBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassBackdrop(layer) }
}

/** Fondo vigente para los hijos; null = no hay nada que desenfocar (cristal opaco). */
val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }

/** Alto que ocupa la barra de pestañas flotante (más la barra de navegación): el contenido lo suma a su relleno inferior. */
val LocalBottomInset = staticCompositionLocalOf { 0.dp }

/** Marca el contenido que las superficies de cristal desenfocarán. */
fun Modifier.glassSource(backdrop: GlassBackdrop): Modifier = this
    .onGloballyPositioned { backdrop.origin = it.positionInRoot() }
    .drawWithContent {
        backdrop.layer.record { this@drawWithContent.drawContent() }
        drawLayer(backdrop.layer)
    }

/**
 * Superficie de cristal: contenido de detrás desenfocado + velo de la superficie + filete con brillo arriba.
 * Úsala solo en capas que flotan (barras, hojas, burbujas); nunca en listas ni en texto largo.
 */
@Composable
fun Modifier.glass(shape: Shape, blur: Dp = 22.dp, tint: Color? = null, outline: Boolean = true): Modifier {
    val backdrop = LocalGlassBackdrop.current
    val dark = LocalDark.current
    val canBlur = backdrop != null && Build.VERSION.SDK_INT >= 31
    val base = tint ?: MaterialTheme.colorScheme.surface
    val screenBg = tint ?: MaterialTheme.colorScheme.background
    val veil = base.copy(alpha = if (!canBlur) 0.96f else if (dark) 0.6f else 0.66f)
    val top = if (dark) Color(0x24FFFFFF) else Color(0xE6FFFFFF)
    val edge = if (dark) Color(0x1FECEFF4) else Color(0x1A14171C)
    val own = rememberGraphicsLayer()
    var pos by remember { mutableStateOf(Offset.Zero) }
    return this
        .onGloballyPositioned { pos = it.positionInRoot() }
        .clip(shape)
        .drawBehind {
            if (canBlur && backdrop != null) {
                // Base opaca (el fondo de la pantalla): la capa grabada solo contiene el contenido, sin fondo; sin esto el
                // contenido NÍTIDO de debajo se transparenta a través del velo y compite con el desenfocado.
                drawRect(screenBg)
                val r = blur.toPx()
                own.renderEffect = BlurEffect(r, r, TileMode.Clamp)
                val dx = backdrop.origin.x - pos.x
                val dy = backdrop.origin.y - pos.y
                val density = this
                own.record(density, layoutDirection, IntSize(size.width.toInt(), size.height.toInt())) {
                    translate(dx, dy) { drawLayer(backdrop.layer) }
                }
                drawLayer(own)
            }
            drawRect(veil)
        }
        .then(if (outline) Modifier.border(1.dp, Brush.verticalGradient(listOf(top, edge)), shape) else Modifier)
}

/** Barra de cristal a todo el ancho (título superior): sin contorno, solo un filete en el borde que da al contenido. */
@Composable
fun Modifier.glassBar(atBottom: Boolean = true): Modifier {
    val line = if (LocalDark.current) Color(0x1FECEFF4) else Color(0x1A14171C)
    return this.glass(RectangleShape, outline = false).drawBehind {
        val h = 1.dp.toPx()
        drawRect(line, topLeft = Offset(0f, if (atBottom) size.height - h else 0f), size = androidx.compose.ui.geometry.Size(size.width, h))
    }
}
