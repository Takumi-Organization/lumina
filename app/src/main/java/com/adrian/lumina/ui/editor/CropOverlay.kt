package com.adrian.lumina.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.adrian.lumina.editor.CropHandle
import com.adrian.lumina.editor.CropRect
import com.adrian.lumina.editor.dragCrop
import com.adrian.lumina.editor.hitHandle

/**
 * Rectángulo de recorte arrastrable sobre una imagen mostrada con «Fit» y [aspect] = ancho/alto.
 * Las coordenadas que emite son normalizadas (0..1) respecto a la imagen, no a la pantalla.
 */
@Composable
fun CropOverlay(rect: CropRect, aspect: Float, color: Color, onChange: (CropRect) -> Unit, onEnd: () -> Unit, modifier: Modifier = Modifier) {
    val current by rememberUpdatedState(rect)
    var handle by remember { mutableStateOf<CropHandle?>(null) }
    Canvas(
        modifier.fillMaxSize().testTag("crop-overlay").pointerInput(aspect) {
            // Zona real de la imagen dentro del lienzo (Fit centrado).
            fun fit(): androidx.compose.ui.geometry.Rect {
                val boxAspect = size.width.toFloat() / size.height
                val w = if (aspect > boxAspect) size.width.toFloat() else size.height * aspect
                val h = if (aspect > boxAspect) size.width / aspect else size.height.toFloat()
                val l = (size.width - w) / 2f
                val t = (size.height - h) / 2f
                return androidx.compose.ui.geometry.Rect(l, t, l + w, t + h)
            }
            detectDragGestures(
                onDragStart = { p ->
                    val f = fit()
                    handle = hitHandle(current, (p.x - f.left) / f.width, (p.y - f.top) / f.height, slop = 0.07f)
                },
                onDragEnd = { handle = null; onEnd() },
                onDragCancel = { handle = null; onEnd() },
                onDrag = { change, drag ->
                    val h = handle ?: return@detectDragGestures
                    change.consume()
                    val f = fit()
                    onChange(dragCrop(current, h, drag.x / f.width, drag.y / f.height))
                },
            )
        },
    ) {
        val boxAspect = size.width / size.height
        val w = if (aspect > boxAspect) size.width else size.height * aspect
        val h = if (aspect > boxAspect) size.width / aspect else size.height
        val ox = (size.width - w) / 2f
        val oy = (size.height - h) / 2f
        val l = ox + rect.left * w; val t = oy + rect.top * h
        val r = ox + rect.right * w; val b = oy + rect.bottom * h
        val dim = Color(0x99000000)
        drawRect(dim, Offset(ox, oy), Size(w, t - oy))
        drawRect(dim, Offset(ox, b), Size(w, oy + h - b))
        drawRect(dim, Offset(ox, t), Size(l - ox, b - t))
        drawRect(dim, Offset(r, t), Size(ox + w - r, b - t))
        drawRect(color, Offset(l, t), Size(r - l, b - t), style = Stroke(3f))
        // Tercios y agarres de las esquinas.
        for (i in 1..2) {
            drawLine(color.copy(alpha = .5f), Offset(l + (r - l) * i / 3, t), Offset(l + (r - l) * i / 3, b), 1.5f)
            drawLine(color.copy(alpha = .5f), Offset(l, t + (b - t) * i / 3), Offset(r, t + (b - t) * i / 3), 1.5f)
        }
        for (cx in listOf(l, r)) for (cy in listOf(t, b)) drawCircle(color, 14f, Offset(cx, cy))
    }
}
