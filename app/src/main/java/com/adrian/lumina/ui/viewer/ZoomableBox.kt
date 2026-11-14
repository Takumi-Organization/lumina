package com.adrian.lumina.ui.viewer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlinx.coroutines.launch

internal const val MAX_ZOOM = 6f
internal const val DOUBLE_TAP_ZOOM = 2.5f
private const val ZOOMED = 1.01f
private const val MIN_ELASTIC = 0.7f
private const val MAX_ELASTIC = MAX_ZOOM * 1.3f

/**
 * Zoom con pellizco, arrastre, doble toque e inercia. Física de muelle: pasarse del límite (zoom o desplazamiento) cuesta
 * cada vez más («goma elástica») y al soltar vuelve con un muelle; al soltar el dedo con el zoom activo la imagen sigue
 * deslizándose con fricción. Con un dedo y sin zoom no consume eventos (el paginador recibe el deslizamiento).
 */
@Composable
fun ZoomableBox(
    onTap: () -> Unit,
    onZoomedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()
    val spring = remember { spring<Float>(dampingRatio = 0.8f, stiffness = 380f) }

    fun bounds(s: Float) = Offset(size.width * (s - 1f).coerceAtLeast(0f) / 2f, size.height * (s - 1f).coerceAtLeast(0f) / 2f)
    fun clamp(o: Offset, s: Float): Offset { val b = bounds(s); return Offset(o.x.coerceIn(-b.x, b.x), o.y.coerceIn(-b.y, b.y)) }
    /** Fuera de límites solo se concede el 35 % del exceso: sensación de goma. */
    fun elastic(o: Offset, s: Float): Offset { val c = clamp(o, s); return c + (o - c) * 0.35f }

    suspend fun settle() {
        val s1 = scale.coerceIn(1f, MAX_ZOOM)
        val o1 = clamp(offset, s1)
        val s0 = scale; val o0 = offset
        if (s0 == s1 && o0 == o1) return
        androidx.compose.animation.core.animate(0f, 1f, animationSpec = spring) { f, _ ->
            scale = s0 + (s1 - s0) * f
            offset = o0 + (o1 - o0) * f
        }
    }

    LaunchedEffect(scale) { onZoomedChange(scale > ZOOMED) }

    Box(
        modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { tap ->
                        scope.launch {
                            val s0 = scale
                            val o0 = offset
                            val s1 = if (s0 > ZOOMED) 1f else DOUBLE_TAP_ZOOM
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val o1 = if (s1 == 1f) Offset.Zero else clamp((center - tap) * (s1 - 1f), s1)
                            androidx.compose.animation.core.animate(0f, 1f, animationSpec = spring) { f, _ ->
                                scale = s0 + (s1 - s0) * f
                                offset = o0 + (o1 - o0) * f
                            }
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val tracker = VelocityTracker()
                    var active = false
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.size >= 2 || scale > ZOOMED) {
                            active = true
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = false)
                            val newScale = (scale * zoom).coerceIn(MIN_ELASTIC, MAX_ELASTIC)
                            val c = Offset(size.width / 2f, size.height / 2f)
                            offset = elastic((offset - (centroid - c)) * (newScale / scale) + (centroid - c) + pan, newScale)
                            scale = newScale
                            event.changes.firstOrNull()?.let { tracker.addPosition(it.uptimeMillis, it.position) }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                    if (active) {
                        val v = tracker.calculateVelocity()
                        scope.launch {
                            if (scale in ZOOMED..MAX_ZOOM && (abs(v.x) > 200f || abs(v.y) > 200f)) {
                                // Inercia: el desplazamiento sigue con fricción y se detiene en el borde de la imagen.
                                val a = Animatable(offset, Offset.VectorConverter)
                                a.animateDecay(Offset(v.x, v.y), exponentialDecay(frictionMultiplier = 1.6f)) { offset = clamp(value, scale) }
                            }
                            settle()
                        }
                    }
                }
            }
            .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y },
    ) { content() }
}
