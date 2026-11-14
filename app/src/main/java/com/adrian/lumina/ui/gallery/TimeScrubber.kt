package com.adrian.lumina.ui.gallery

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.draw.shadow
import com.adrian.lumina.ui.components.glass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.adrian.lumina.domain.model.Timeline
import com.adrian.lumina.domain.usecase.entryAtFraction
import com.adrian.lumina.domain.usecase.fractionOfEntry
import com.adrian.lumina.domain.usecase.monthAtEntry
import com.adrian.lumina.domain.usecase.yearTicks
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Carril temporal a la derecha: años en Anton alineados con su posición real, barra de acento cuadrada y, al arrastrar,
 * un cartel gigante con el mes y el año (la biblioteca como cinta de contactos). Visible al desplazarse o arrastrar.
 */
@Composable
fun TimeScrubber(timeline: Timeline, list: LazyListState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var heightPx by remember { mutableFloatStateOf(1f) }
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var visible by remember { mutableStateOf(false) }

    val scrolling = list.isScrollInProgress
    LaunchedEffect(scrolling, dragging) {
        if (scrolling || dragging) visible = true else { delay(1200); visible = false }
    }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, label = "scrubAlpha")

    val scrollFraction by remember(timeline) { derivedStateOf { timeline.fractionOfEntry(list.firstVisibleItemIndex) } }
    val fraction = if (dragging) dragFraction else scrollFraction
    val ticks = remember(timeline) { timeline.yearTicks() }
    val density = LocalDensity.current
    val thumbH = 44.dp
    val thumbPx = with(density) { thumbH.toPx() }
    val usable = (heightPx - thumbPx).coerceAtLeast(1f)

    fun jump(y: Float) {
        val f = ((y - thumbPx / 2) / usable).coerceIn(0f, 1f)
        dragFraction = f
        scope.launch { list.scrollToItem(timeline.entryAtFraction(f)) }
    }

    Box(
        modifier
            .testTag("scrubber")
            .fillMaxHeight()
            .width(36.dp)
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
            var lastY = -1000f
            val minGap = with(density) { 22.dp.toPx() }
            ticks.forEach { tick ->
                val y = tick.fraction * usable + thumbPx / 2
                if (y - lastY >= minGap) {
                    lastY = y
                    Text(
                        "${tick.year}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.align(Alignment.TopEnd)
                            .wrapContentWidth(Alignment.End, unbounded = true)
                            .offset { IntOffset(-with(density) { 14.dp.roundToPx() }, (y - with(density) { 8.dp.toPx() }).roundToInt()) }
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(glass())
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }
            Box(
                Modifier.align(Alignment.TopEnd)
                    .offset { IntOffset(0, (fraction * usable).roundToInt()) }
                    .padding(end = 3.dp).width(5.dp).height(thumbH)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        if (dragging) {
            val mark = timeline.monthAtEntry(timeline.entryAtFraction(fraction))
            if (mark != null) MonthPoster(mark.month, Modifier.align(Alignment.TopEnd).offset { IntOffset(0, (fraction * usable).roundToInt()) })
        }
    }
}

/** Cartel que sale al arrastrar: tarjeta de esquinas continuas con el mes y el año, pegada al carril. */
@Composable
private fun MonthPoster(month: YearMonth, modifier: Modifier) {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    Column(
        modifier.wrapContentWidth(Alignment.End, unbounded = true).offset(x = (-28).dp)
            .shadow(20.dp, com.adrian.lumina.ui.theme.SquircleShape(18.dp))
            .clip(com.adrian.lumina.ui.theme.SquircleShape(18.dp)).background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 18.dp, vertical = 8.dp).testTag("scrubber-bubble"),
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            month.month.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.uppercase(locale) },
            style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimary, maxLines = 1, softWrap = false,
        )
        Text("${month.year}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
    }
}
