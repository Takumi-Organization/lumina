package com.adrian.lumina.ui.gallery

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adrian.lumina.domain.model.Timeline
import com.adrian.lumina.domain.usecase.entryAtFraction
import com.adrian.lumina.domain.usecase.fractionOfEntry
import com.adrian.lumina.domain.usecase.monthAtEntry
import com.adrian.lumina.domain.usecase.yearTicks
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Carril temporal a la derecha: arrastrar salta por la biblioteca; marcas por año y burbuja
 * con mes. Visible al desplazarse o arrastrar, se oculta tras 1,2 s de reposo.
 */
@Composable
fun TimeScrubber(timeline: Timeline, grid: LazyGridState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var heightPx by remember { mutableFloatStateOf(1f) }
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var visible by remember { mutableStateOf(false) }

    val scrolling = grid.isScrollInProgress
    LaunchedEffect(scrolling, dragging) {
        if (scrolling || dragging) visible = true else { delay(1200); visible = false }
    }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, label = "scrubAlpha")

    val scrollFraction by remember(timeline) {
        derivedStateOf { timeline.fractionOfEntry(grid.firstVisibleItemIndex) }
    }
    val fraction = if (dragging) dragFraction else scrollFraction
    val ticks = remember(timeline) { timeline.yearTicks() }
    val density = LocalDensity.current
    val thumbH = 40.dp
    val usable = (heightPx - with(density) { thumbH.toPx() }).coerceAtLeast(1f)

    fun jump(y: Float) {
        val f = ((y - with(density) { thumbH.toPx() } / 2) / usable).coerceIn(0f, 1f)
        dragFraction = f
        scope.launch { grid.scrollToItem(timeline.entryAtFraction(f)) }
    }

    Box(
        modifier
            .testTag("scrubber")
            .fillMaxHeight()
            .width(32.dp)
            .onSizeChanged { heightPx = it.height.toFloat() }
            .pointerInput(timeline) {
                detectVerticalDragGestures(
                    onDragStart = { dragging = true; jump(it.y) },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                    onVerticalDrag = { change, _ -> change.consume(); jump(change.position.y) },
                )
            },
    ) {
        Box(Modifier.fillMaxSize().alpha(alpha)) {
            // Marcas de año alineadas con la posición real de cada año.
            var lastY = -1000f
            val minGap = with(density) { 18.dp.toPx() }
            ticks.forEach { tick ->
                val y = tick.fraction * usable + with(density) { thumbH.toPx() } / 2
                if (y - lastY >= minGap) {
                    lastY = y
                    Text(
                        "${tick.year}", fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.TopEnd)
                            .wrapContentWidth(Alignment.End, unbounded = true)
                            .offset { IntOffset(-with(density) { 14.dp.roundToPx() }, (y - with(density) { 8.dp.toPx() }).roundToInt()) }
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
            }
            Box(
                Modifier.align(Alignment.TopEnd).padding(end = 4.dp)
                    .offset { IntOffset(0, (fraction * usable).roundToInt()) }
                    .width(5.dp).height(thumbH)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        if (dragging) {
            val month = timeline.monthAtEntry(timeline.entryAtFraction(fraction))
            if (month != null) {
                Text(
                    monthLabel(month.month),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1, softWrap = false,
                    modifier = Modifier.align(Alignment.TopEnd)
                        .wrapContentWidth(Alignment.End, unbounded = true)
                        .offset { IntOffset(-with(density) { 20.dp.roundToPx() }, (fraction * usable).roundToInt() + with(density) { 8.dp.roundToPx() }) }
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("scrubber-bubble"),
                )
            }
        }
    }
}

internal fun monthLabel(month: YearMonth, locale: Locale = Locale.getDefault()): String {
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "MMMMyyyy")
    return month.format(DateTimeFormatter.ofPattern(pattern, locale)).replaceFirstChar { it.uppercase(locale) }
}
