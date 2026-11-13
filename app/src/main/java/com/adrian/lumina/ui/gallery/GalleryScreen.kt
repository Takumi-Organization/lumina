package com.adrian.lumina.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.adrian.lumina.data.media.Thumb
import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.TimelineEntry
import com.adrian.lumina.ui.components.EmptyState
import com.adrian.lumina.ui.components.GridSkeleton
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val COLUMNS = 4

@Composable
fun GalleryScreen(state: GalleryUiState, modifier: Modifier = Modifier) {
    val pad = PaddingValues(top = 104.dp, bottom = 24.dp, start = 2.dp, end = 2.dp)
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (state) {
            GalleryUiState.Loading -> GridSkeleton(COLUMNS, pad)
            GalleryUiState.Empty -> EmptyState(
                title = "Aún no hay nada que revelar",
                body = "Cuando hagas fotos o grabes vídeos aparecerán aquí, ordenados por fecha.",
            )
            is GalleryUiState.Content -> Grid(state, pad)
        }
        TopTitle(state)
    }
}

@Composable
private fun TopTitle(state: GalleryUiState) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
            .statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text("Lumina", style = MaterialTheme.typography.headlineMedium)
        if (state is GalleryUiState.Content) {
            Text(
                "  ${state.total}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
    }
}

@Composable
private fun Grid(state: GalleryUiState.Content, pad: PaddingValues) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(COLUMNS),
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        contentPadding = pad,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(
            items = state.timeline.entries,
            key = { it.key },
            span = { if (it is TimelineEntry.Header) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
            contentType = { if (it is TimelineEntry.Header) "h" else "c" },
        ) { entry ->
            when (entry) {
                is TimelineEntry.Header -> DayHeader(entry.day, entry.count)
                is TimelineEntry.Cell -> Cell(entry.item)
            }
        }
    }
}

@Composable
private fun DayHeader(day: LocalDate, count: Int) {
    Row(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(dayLabel(day), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text("$count", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Cell(item: MediaItem) {
    Box(Modifier.aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceVariant)) {
        AsyncImage(
            model = Thumb(item.uri),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (item.isVideo) {
            Row(
                Modifier.align(Alignment.BottomStart).padding(4.dp)
                    .clip(RoundedCornerShape(50)).background(Color(0x99000000))
                    .padding(start = 3.dp, end = 7.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.padding(end = 1.dp))
                Text(formatDuration(item.durationMs), color = Color.White, fontSize = 11.sp)
            }
        }
    }
}

internal fun formatDuration(ms: Long): String {
    val s = ms / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s % 3600 / 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
}

internal fun dayLabel(day: LocalDate, today: LocalDate = LocalDate.now(), locale: Locale = Locale.getDefault()): String {
    val es = locale.language == "es"
    return when (day) {
        today -> if (es) "Hoy" else "Today"
        today.minusDays(1) -> if (es) "Ayer" else "Yesterday"
        else -> {
            // Patrón local (orden y preposiciones según idioma), no literales fijos.
            val skeleton = if (day.year == today.year) "EEEEdMMMM" else "dMMMMyyyy"
            val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton)
            day.format(DateTimeFormatter.ofPattern(pattern, locale)).replaceFirstChar { it.uppercase(locale) }
        }
    }
}
