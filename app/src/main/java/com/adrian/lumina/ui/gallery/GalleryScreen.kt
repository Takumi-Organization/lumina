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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material.icons.outlined.Share
import androidx.compose.ui.graphics.Color
import com.adrian.lumina.domain.usecase.monthAtEntry
import com.adrian.lumina.ui.components.glass
import com.adrian.lumina.ui.theme.Springs
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.adrian.lumina.R
import com.adrian.lumina.domain.model.TimelineEntry
import com.adrian.lumina.ui.components.EmptyState
import com.adrian.lumina.domain.usecase.overview
import com.adrian.lumina.ui.components.Hairline
import java.time.LocalDate
import java.time.format.TextStyle
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
    val onRetry: () -> Unit = {},
    val onOpenPermissions: () -> Unit = {},
    val onPickMonth: (com.adrian.lumina.domain.usecase.MonthOverview) -> Unit = {},
    val onPickYear: (com.adrian.lumina.domain.usecase.YearOverview) -> Unit = {},
)

private val TitleBarHeight = 64.dp
private const val GAP = 3

@Composable
fun GalleryScreen(
    state: GalleryUiState,
    selection: Set<Long>,
    columns: Int,
    favoritesOnly: Boolean,
    allSelectedFavorite: Boolean,
    actions: GalleryActions,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    level: com.adrian.lumina.domain.usecase.ZoomLevel = com.adrian.lumina.domain.usecase.ZoomLevel.DAYS,
) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TitleBarHeight
    val pad = PaddingValues(top = top, bottom = 24.dp)
    val selecting = selection.isNotEmpty()
    BackHandler(enabled = selecting) { actions.onClearSelection() }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (state) {
            GalleryUiState.Loading -> LoadingMarks(pad)
            is GalleryUiState.Empty -> if (state.favoritesOnly) EmptyState(
                title = stringResource(R.string.empty_favorites_title),
                body = stringResource(R.string.empty_favorites_body),
            ) else EmptyState(
                title = stringResource(R.string.empty_library_title),
                body = stringResource(R.string.empty_library_body),
                detail = state.detail.takeIf { it.isNotBlank() }?.let { stringResource(R.string.library_detail, it) },
                actionLabel = stringResource(R.string.library_retry), onAction = actions.onRetry,
                secondaryLabel = stringResource(R.string.library_permissions), onSecondary = actions.onOpenPermissions,
            )
            is GalleryUiState.Failed -> EmptyState(
                title = stringResource(R.string.library_failed_title),
                body = stringResource(R.string.library_failed_body, state.message),
                detail = state.detail.takeIf { it.isNotBlank() }?.let { stringResource(R.string.library_detail, it) },
                actionLabel = stringResource(R.string.library_retry), onAction = actions.onRetry,
                secondaryLabel = stringResource(R.string.library_permissions), onSecondary = actions.onOpenPermissions,
            )
            is GalleryUiState.Content -> {
                androidx.compose.runtime.LaunchedEffect(Unit) { com.adrian.lumina.app.PerfLog.mark("lista compuesta") }
                val currentColumns by rememberUpdatedState(columns)
                val onColumns by rememberUpdatedState(actions.onColumns)
                val overview = androidx.compose.runtime.remember(state.timeline) { state.timeline.overview() }
                val sidePad = PaddingValues(top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding(), start = 14.dp, end = 14.dp)
                androidx.compose.animation.AnimatedContent(
                    targetState = level,
                    modifier = Modifier.fillMaxSize().pinchToChangeColumns(columns = { currentColumns }, onColumns = { onColumns(it) }),
                    transitionSpec = {
                        if (targetState.ordinal < initialState.ordinal) {
                            // Hacia años: la vista actual se aleja y la nueva llega desde dentro.
                            (fadeIn(Springs.smooth()) + androidx.compose.animation.scaleIn(Springs.smooth(), initialScale = 1.18f)) togetherWith
                                (fadeOut(Springs.smooth()) + androidx.compose.animation.scaleOut(Springs.smooth(), targetScale = 0.86f))
                        } else {
                            (fadeIn(Springs.smooth()) + androidx.compose.animation.scaleIn(Springs.smooth(), initialScale = 0.86f)) togetherWith
                                (fadeOut(Springs.smooth()) + androidx.compose.animation.scaleOut(Springs.smooth(), targetScale = 1.18f))
                        }
                    },
                    label = "zoomLevel",
                ) { lv ->
                    when (lv) {
                        com.adrian.lumina.domain.usecase.ZoomLevel.DAYS -> Box(Modifier.fillMaxSize()) {
                            Mosaic(state, selection, columns, actions, listState, pad)
                            TimeScrubber(
                                state.timeline, listState,
                                Modifier.align(Alignment.CenterEnd).statusBarsPadding().padding(top = TitleBarHeight).navigationBarsPadding(),
                            )
                        }
                        com.adrian.lumina.domain.usecase.ZoomLevel.MONTHS -> MonthsView(overview, sidePad, actions.onPickMonth)
                        com.adrian.lumina.domain.usecase.ZoomLevel.YEARS -> YearsView(overview, pad, actions.onPickYear)
                    }
                }
            }
        }
        AnimatedContent(
            targetState = selecting,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "topbar",
        ) { sel ->
            if (sel) SelectionBar(selection.size, allSelectedFavorite, actions)
            else TitleBar(state, favoritesOnly, listState, actions)
        }
    }
}

@Composable
private fun TitleBar(state: GalleryUiState, favoritesOnly: Boolean, listState: LazyListState, actions: GalleryActions) {
    val timeline = (state as? GalleryUiState.Content)?.timeline
    // Título grande en la cima; al desplazarse se transforma en el mes actual (fondo translúcido con sombra).
    val atTop by androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 40 } }
    val mark = timeline?.let { t -> androidx.compose.runtime.remember(t, listState.firstVisibleItemIndex) { t.monthAtEntry(listState.firstVisibleItemIndex) } }
    val bg by androidx.compose.animation.animateColorAsState(
        if (atTop) Color.Transparent else glass(), Springs.smooth(), label = "barBg",
    )
    Row(
        Modifier.fillMaxWidth().background(bg).statusBarsPadding().padding(start = 20.dp, end = 6.dp, top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.animation.AnimatedContent(
            targetState = if (atTop || mark == null) null else mark.month,
            transitionSpec = {
                (androidx.compose.animation.slideInVertically(Springs.snappy()) { it / 2 } + fadeIn()) togetherWith
                    (androidx.compose.animation.slideOutVertically(Springs.snappy()) { -it / 2 } + fadeOut())
            },
            modifier = Modifier.weight(1f), label = "barTitle",
        ) { month ->
            if (month == null) Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    if (favoritesOnly) stringResource(R.string.title_favorites) else stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                )
                (state as? GalleryUiState.Content)?.total?.let {
                    Text("$it", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 10.dp, bottom = 6.dp))
                }
            } else Text(monthTitle(month), style = MaterialTheme.typography.titleLarge)
        }
        IconButton(onClick = { actions.onFavoritesOnly(!favoritesOnly) }, modifier = Modifier.testTag("filter-favorites")) {
            Icon(
                if (favoritesOnly) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                stringResource(if (favoritesOnly) R.string.filter_show_all else R.string.filter_only_favorites),
                tint = if (favoritesOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun monthTitle(m: java.time.YearMonth): String {
    val l = Locale.getDefault()
    return m.month.getDisplayName(TextStyle.FULL, l).replaceFirstChar { it.uppercase(l) } + " " + m.year
}

@Composable
private fun SelectionBar(count: Int, allFavorite: Boolean, actions: GalleryActions) {
    Row(
        Modifier.fillMaxWidth().background(glass()).statusBarsPadding().padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = actions.onClearSelection, modifier = Modifier.testTag("selection-close")) {
            Icon(Icons.Outlined.Close, stringResource(R.string.cancel_selection))
        }
        Text(
            pluralStringResource(R.plurals.selected_count, count, count),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).testTag("selection-count"),
        )
        IconButton(onClick = actions.onAddToAlbum, modifier = Modifier.testTag("selection-album")) {
            Icon(Icons.Outlined.PhotoAlbum, stringResource(R.string.album_add_to))
        }
        IconButton(onClick = actions.onShareSelected, modifier = Modifier.testTag("selection-share")) {
            Icon(Icons.Outlined.Share, stringResource(R.string.share))
        }
        IconButton(onClick = actions.onDeleteSelected, modifier = Modifier.testTag("selection-delete")) {
            Icon(Icons.Outlined.Delete, stringResource(R.string.delete))
        }
        IconButton(onClick = actions.onFavoriteSelected, modifier = Modifier.testTag("selection-favorite")) {
            Icon(
                if (allFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                stringResource(if (allFavorite) R.string.remove_from_favorites else R.string.add_to_favorites),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** Marcas de carga: rectángulos de mosaico con pulso de opacidad (sin degradados). */
@Composable
private fun LoadingMarks(pad: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(pad), verticalArrangement = Arrangement.spacedBy(GAP.dp)) {
        repeat(5) { r ->
            Row(Modifier.fillMaxWidth().aspectRatio(if (r % 2 == 0) 3f else 2.2f), horizontalArrangement = Arrangement.spacedBy(GAP.dp)) {
                repeat(if (r % 2 == 0) 3 else 2) { Box(Modifier.weight(1f).fillMaxHeight().clip(androidx.compose.foundation.shape.RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) }
            }
        }
    }
}

@Composable
private fun Mosaic(
    state: GalleryUiState.Content,
    selection: Set<Long>,
    columns: Int,
    actions: GalleryActions,
    listState: LazyListState,
    pad: PaddingValues,
) {
    val selecting = selection.isNotEmpty()
    val today = androidx.compose.runtime.remember { LocalDate.now() }
    val todayLabel = stringResource(R.string.today)
    val yesterdayLabel = stringResource(R.string.yesterday)
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        contentPadding = pad,
        verticalArrangement = Arrangement.spacedBy(GAP.dp),
    ) {
        items(
            count = state.timeline.entries.size,
            key = { state.timeline.entries[it].key },
            contentType = { when (state.timeline.entries[it]) { is TimelineEntry.Month -> 0; is TimelineEntry.Day -> 1; is TimelineEntry.Row -> 2 } },
        ) { index ->
            when (val entry = state.timeline.entries[index]) {
                is TimelineEntry.Month -> MonthHeader(entry)
                is TimelineEntry.Day -> DayTag(
                    entry, selecting, allSelected = selecting && selection.containsAll(entry.ids),
                    label = dayLabel(entry.day, todayLabel, yesterdayLabel, today),
                    onClick = { if (selecting) actions.onToggleDay(entry.ids) },
                )
                is TimelineEntry.Row -> MosaicRow(entry, selection, selecting, actions)
            }
        }
    }
}

@Composable
private fun MosaicRow(row: TimelineEntry.Row, selection: Set<Long>, selecting: Boolean, actions: GalleryActions) {
    Row(Modifier.fillMaxWidth().aspectRatio(row.aspect), horizontalArrangement = Arrangement.spacedBy(GAP.dp)) {
        for (cell in row.cells) {
            androidx.compose.runtime.key(cell.item.id) {
                GalleryCell(
                    item = cell.item,
                    selected = cell.item.id in selection,
                    selecting = selecting,
                    onClick = { if (selecting) actions.onToggle(cell.item.id) else actions.onOpen(cell.item.id) },
                    onLongClick = { actions.onToggle(cell.item.id) },
                    modifier = Modifier.weight(cell.aspect).fillMaxHeight(),
                    // Fila hero: una sola foto a todo ancho → miniatura más grande para que no se vea pixelada.
                    thumbPx = if (row.hero) 640 else com.adrian.lumina.ui.theme.LocalThumbSize.current,
                )
            }
        }
        if (row.leftover > 0.01f) {
            // Hueco de la última fila del día: el número del día en gigante, como sello de la hoja de contactos.
            Box(Modifier.weight(row.leftover).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                if (row.leftover / row.aspect >= 0.3f) Column(Modifier.padding(start = 14.dp)) {
                    Text(
                        "${row.day.dayOfMonth}", style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f), maxLines = 1, softWrap = false,
                    )
                    Text(
                        row.day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Cabecera de mes: nombre en peso 800 a 40 sp y año en gris, como los titulares de Apple Fotos. Recuento a la derecha. */
@Composable
private fun MonthHeader(entry: TimelineEntry.Month) {
    val locale = Locale.getDefault()
    val name = entry.month.month.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.uppercase(locale) }
    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 30.dp, bottom = 4.dp), verticalAlignment = Alignment.Bottom) {
        Text(
            buildAnnotatedString {
                append(name)
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Light)) { append(" ${entry.month.year}") }
            },
            style = MaterialTheme.typography.displayMedium, maxLines = 1, modifier = Modifier.weight(1f),
        )
        Text("${entry.count}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
    }
}

/** Marca de día: «Jueves 12» en semibold; en selección ofrece elegir el día entero con un botón de acento. */
@Composable
private fun DayTag(entry: TimelineEntry.Day, selecting: Boolean, allSelected: Boolean, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1)
        if (selecting) {
            Text(
                stringResource(if (allSelected) R.string.deselect_day else R.string.select_day),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(androidx.compose.foundation.shape.CircleShape).clickable(onClick = onClick)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)).padding(horizontal = 12.dp, vertical = 5.dp),
            )
        } else Text("${entry.count}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Pellizco con dos dedos: cambia la densidad del mosaico (más separados = fotos más grandes).
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

internal fun dayLabel(
    day: LocalDate, todayLabel: String, yesterdayLabel: String,
    today: LocalDate = LocalDate.now(), locale: Locale = Locale.getDefault(),
): String = when (day) {
    today -> todayLabel
    today.minusDays(1) -> yesterdayLabel
    else -> day.dayOfWeek.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.uppercase(locale) } + " " + day.dayOfMonth
}
