package com.adrian.lumina.ui.theme

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Esquinas continuas («squircle», como iOS): la curvatura crece de forma gradual en lugar de saltar de la recta al arco.
 * Cada esquina es un cuarto de superelipse (exponente 4) que se extiende un 50 % más que un círculo del mismo radio.
 */
class SquircleShape(private val radius: Dp = 20.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { radius.toPx() }
        val p = (r * 1.5f).coerceAtMost(minOf(size.width, size.height) / 2f)
        if (p <= 0f) return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        return Outline.Generic(buildPath(size.width, size.height, p))
    }

    companion object {
        private const val E = 2f / 4f // 2/n con n = 4
        private const val STEPS = 16

        /** Camino de un rectángulo de [w]×[h] con esquinas de extensión [p]. Público para poder probarlo. */
        fun buildPath(w: Float, h: Float, p: Float): Path = Path().apply {
            fun c(t: Float) = cos(t).coerceAtLeast(0f).pow(E)
            fun s(t: Float) = sin(t).coerceAtLeast(0f).pow(E)
            fun arc(cx: Float, cy: Float, fx: (Float) -> Float, fy: (Float) -> Float, first: Boolean) {
                for (i in 0..STEPS) {
                    val t = (Math.PI / 2 * i / STEPS).toFloat()
                    val x = cx + p * fx(t); val y = cy + p * fy(t)
                    if (first && i == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            arc(p, p, { -c(it) }, { -s(it) }, first = true)            // sup. izquierda: de (0,p) a (p,0)
            arc(w - p, p, { s(it) }, { -c(it) }, first = false)        // sup. derecha: de (w-p,0) a (w,p)
            arc(w - p, h - p, { c(it) }, { s(it) }, first = false)     // inf. derecha: de (w,h-p) a (w-p,h)
            arc(p, h - p, { -s(it) }, { c(it) }, first = false)        // inf. izquierda: de (p,h) a (0,h-p)
            close()
        }
    }
}
