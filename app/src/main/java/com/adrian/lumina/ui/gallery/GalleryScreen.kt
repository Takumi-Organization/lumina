package com.adrian.lumina.ui.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.adrian.lumina.R
import androidx.compose.ui.unit.dp
import com.adrian.lumina.domain.model.TimelineEntry
import com.adrian.lumina.ui.components.EmptyState
import com.adrian.lumina.ui.components.GridSkeleton
import java.time.LocalDate
import java.util.Locale

/** Acciones de la galería: la pantalla es sin estado para poder probarla. */
class GalleryActions(
    val onToggle: (Long) -> Unit = {},
    val onToggleDay: (List<Long>) -> Unit = {},
    val onClearSelection: () -> Unit = {},
    val onFavoriteSelected: () -> Unit = {},
    val onFavoritesOnly: (Boolean) -> Unit = {},
    val onColumns: (Int) -> Unit = {},
    val onOpen: (Long) -> Unit = {},
    val onShareSelected: () -> Unit = {},
    val onDeleteSelected: () -> Unit = {},
    val onAddToAlbum: () -> Unit = {},
)

private val TopInset = 104.dp

@Composable
fun GalleryScreen(
    state: GalleryUiState,
    selection: Set<Long>,
    columns: Int,
    favoritesOnly: Boolean,
    allSelectedFavorite: Boolean,
    actions: GalleryActions,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
) {
    val pad = PaddingValues(top = TopInset, bottom = 24.dp, start = 2.dp, end = 2.dp)
    val selecting = selection.isNotEmpty()
    BackHandler(enabled = selecting) { actions.onClearSelection() }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (state) {
            GalleryUiState.Loading -> GridSkeleton(columns, pad)
            is GalleryUiState.Empty -> if (state.favoritesOnly) EmptyState(
                title = stringResource(R.string.empty_favorites_title),
                body = stringResource(R.string.empty_favorites_body),
            ) else EmptyState(
                title = stringResource(R.string.empty_library_title),
                body = stringResource(R.string.empty_library_body),
            )
            is GalleryUiState.Content -> {
                Grid(state, selection, columns, actions, gridState, pad)
                TimeScrubber(
                    state.timeline, gridState,
                    Modifier.align(Alignment.CenterEnd).statusBarsPadding().padding(top = TopInset - 40.dp).navigationBarsPadding(),
                )
            }
        }
        AnimatedContent(
            targetState = selecting,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "topbar",
        ) { sel ->
            if (sel) SelectionBar(selection.size, allSelectedFavorite, actions)
            else TitleBar(state, favoritesOnly, actions)
        }
    }
}

@Composable
private fun TitleBar(state: GalleryUiState, favoritesOnly: Boolean, actions: GalleryActions) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
            .statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(if (favoritesOnly) stringResource(R.string.title_favorites) else stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        if (state is GalleryUiState.Content) {
            Text(
                "  ${state.total}", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Box(Modifier.weight(1f))
        IconButton(onClick = { actions.onFavoritesOnly(!favoritesOnly) }, modifier = Modifier.testTag("filter-favorites")) {
            Icon(
                if (favoritesOnly) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                stringResource(if (favoritesOnly) R.string.filter_show_all else R.string.filter_only_favorites),
                tint = if (favoritesOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SelectionBar(count: Int, allFavorite: Boolean, actions: GalleryActions) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)
            .statusBarsPadding().padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = actions.onClearSelection, modifier = Modifier.testTag("selection-close")) {
            Icon(Icons.Rounded.Close, stringResource(R.string.cancel_selection))
        }
        Text(
            pluralStringResource(R.plurals.selected_count, count, count),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f).testTag("selection-count"),
        )
        IconButton(onClick = actions.onAddToAlbum, modifier = Modifier.testTag("selection-album")) {
            Icon(Icons.Rounded.PhotoAlbum, stringResource(R.string.album_add_to))
        }
        IconButton(onClick = actions.onShareSelected, modifier = Modifier.testTag("selection-share")) {
            Icon(Icons.Rounded.Share, stringResource(R.string.share))
        }
        IconButton(onClick = actions.onDeleteSelected, modifier = Modifier.testTag("selection-delete")) {
            Icon(Icons.Rounded.Delete, stringResource(R.string.delete))
        }
        IconButton(onClick = actions.onFavoriteSelected, modifier = Modifier.testTag("selection-favorite")) {
            Icon(
                if (allFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                stringResource(if (allFavorite) R.string.remove_from_favorites else R.string.add_to_favorites),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun Grid(
    state: GalleryUiState.Content,
    selection: Set<Long>,
    columns: Int,
    actions: GalleryActions,
    gridState: LazyGridState,
    pad: PaddingValues,
) {
    val selecting = selection.isNotEmpty()
    val currentColumns by rememberUpdatedState(columns)
    val onColumns by rememberUpdatedState(actions.onColumns)
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize().navigationBarsPadding().pinchToChangeColumns(
            columns = { currentColumns }, onColumns = { onColumns(it) },
        ),
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
                is TimelineEntry.Header -> DayHeader(
                    entry.day, entry.count, selecting,
                    allSelected = selecting && selection.containsAll(entry.ids),
                    onClick = { if (selecting) actions.onToggleDay(entry.ids) },
                )
                is TimelineEntry.Cell -> GalleryCell(
                    item = entry.item,
                    selected = entry.item.id in selection,
                    selecting = selecting,
                    onClick = { if (selecting) actions.onToggle(entry.item.id) else actions.onOpen(entry.item.id) },
                    onLongClick = { actions.onToggle(entry.item.id) },
                )
            }
        }
    }
}

/**
 * Pellizco con dos dedos: cambia el número de columnas (más separados = menos columnas).
 * Actúa en la pasada Initial y solo consume con ≥2 dedos, así que el scroll de un dedo no se ve afectado.
 */
private fun Modifier.pinchToChangeColumns(columns: () -> Int, onColumns: (Int) -> Unit): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var accumulated = 1f
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.size >= 2) {
                    accumulated *= event.calculateZoom()
                    when {
                        accumulated > 1.3f -> { onColumns(columns() - 1); accumulated = 1f }
                        accumulated < 0.77f -> { onColumns(columns() + 1); accumulated = 1f }
                    }
                    event.changes.forEach { it.consume() }
                }
            } while (event.changes.any { it.pressed })
        }
    }

@Composable
private fun DayHeader(day: LocalDate, count: Int, selecting: Boolean, allSelected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = selecting, onClick = onClick)
            .padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(dayLabel(day, stringResource(R.string.today), stringResource(R.string.yesterday)), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(
            if (selecting) stringResource(if (allSelected) R.string.deselect_day else R.string.select_day) else "$count",
            style = MaterialTheme.typography.labelMedium,
            color = if (selecting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun dayLabel(
    day: LocalDate, todayLabel: String, yesterdayLabel: String,
    today: LocalDate = LocalDate.now(), locale: Locale = Locale.getDefault(),
): String = when (day) {
    today -> todayLabel
    today.minusDays(1) -> yesterdayLabel
    else -> {
        val skeleton = if (day.year == today.year) "EEEEdMMMM" else "dMMMMyyyy"
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton)
        day.format(java.time.format.DateTimeFormatter.ofPattern(pattern, locale)).replaceFirstChar { it.uppercase(locale) }
    }
}
