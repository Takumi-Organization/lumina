package com.adrian.lumina.ui.albums

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adrian.lumina.R
import com.adrian.lumina.domain.model.AlbumRef
import com.adrian.lumina.domain.model.AlbumSort
import com.adrian.lumina.ui.gallery.GalleryCell

class AlbumDetailActions(
    val onBack: () -> Unit = {},
    val onOpenItem: (Long) -> Unit = {},
    val onToggle: (Long) -> Unit = {},
    val onClearSelection: () -> Unit = {},
    val onSort: (AlbumSort) -> Unit = {},
    val onRemoveSelected: () -> Unit = {},
    val onCoverSelected: () -> Unit = {},
    val onMoveSelected: (Int) -> Unit = {},
    val onEdit: () -> Unit = {},
    val onToggleFavorite: () -> Unit = {},
    val onToggleHidden: () -> Unit = {},
    val onDelete: () -> Unit = {},
)

@Composable
fun AlbumDetailScreen(
    detail: AlbumDetail,
    selection: Set<Long>,
    sort: AlbumSort,
    actions: AlbumDetailActions,
    modifier: Modifier = Modifier,
) {
    val selecting = selection.isNotEmpty()
    val manual = detail.ref is AlbumRef.Manual
    BackHandler { if (selecting) actions.onClearSelection() else actions.onBack() }
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize().navigationBarsPadding().testTag("album-grid"),
            contentPadding = PaddingValues(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 72.dp, bottom = 24.dp, start = 2.dp, end = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            items(detail.items, key = { it.id }) { item ->
                GalleryCell(
                    item, selected = item.id in selection, selecting = selecting,
                    onClick = { if (selecting) actions.onToggle(item.id) else actions.onOpenItem(item.id) },
                    onLongClick = { if (manual) actions.onToggle(item.id) },
                    modifier = Modifier.aspectRatio(1f),
                )
            }
        }
        if (selecting) SelectionBar(selection.size, canMove = manual && sort == AlbumSort.MANUAL, actions)
        else TopBar(detail, sort, actions)
    }
}

@Composable
private fun TopBar(detail: AlbumDetail, sort: AlbumSort, actions: AlbumDetailActions) {
    var menu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    val album = detail.album
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = actions.onBack, modifier = Modifier.testTag("album-back")) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back)) }
        Column(Modifier.weight(1f)) {
            val title = when (val r = detail.ref) {
                is AlbumRef.Manual -> album?.name.orEmpty()
                is AlbumRef.Smart -> stringResource(smartTitleRes(r.type))
                is AlbumRef.Folder -> r.path.trimEnd('/').substringAfterLast('/')
            }
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            val sub = album?.description?.takeIf { it.isNotBlank() }
                ?: pluralStringResource(R.plurals.items_count, detail.items.size, detail.items.size)
            Text(sub, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Box {
            IconButton(onClick = { sortMenu = true }, modifier = Modifier.testTag("album-sort")) { Icon(Icons.AutoMirrored.Outlined.Sort, stringResource(R.string.sort_title)) }
            DropdownMenu(sortMenu, { sortMenu = false }) {
                AlbumSort.entries.forEach { s ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(sortRes(s)),
                                color = if (s == sort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        onClick = { sortMenu = false; actions.onSort(s) },
                    )
                }
            }
        }
        if (album != null) {
            IconButton(onClick = actions.onToggleFavorite, modifier = Modifier.testTag("album-favorite")) {
                Icon(
                    if (album.isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    stringResource(R.string.favorite), tint = MaterialTheme.colorScheme.primary,
                )
            }
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.testTag("album-menu")) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.info)) }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text(stringResource(R.string.album_edit)) }, { menu = false; actions.onEdit() })
                    DropdownMenuItem(
                        { Text(stringResource(if (album.isHidden) R.string.album_unhide else R.string.album_hide)) },
                        { menu = false; actions.onToggleHidden() },
                    )
                    DropdownMenuItem({ Text(stringResource(R.string.album_delete)) }, { menu = false; actions.onDelete() })
                }
            }
        }
    }
}

fun sortRes(s: AlbumSort) = when (s) {
    AlbumSort.MANUAL -> R.string.sort_manual
    AlbumSort.DATE_DESC -> R.string.sort_date_desc
    AlbumSort.DATE_ASC -> R.string.sort_date_asc
    AlbumSort.NAME -> R.string.sort_name
}

@Composable
private fun SelectionBar(count: Int, canMove: Boolean, actions: AlbumDetailActions) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).statusBarsPadding().padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = actions.onClearSelection) { Icon(Icons.Outlined.Close, stringResource(R.string.cancel_selection)) }
        Text(pluralStringResource(R.plurals.selected_count, count, count), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.testTag("album-selection-menu")) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.info)) }
            DropdownMenu(menu, { menu = false }) {
                if (canMove) {
                    DropdownMenuItem({ Text(stringResource(R.string.album_move_earlier)) }, { actions.onMoveSelected(-1) })
                    DropdownMenuItem({ Text(stringResource(R.string.album_move_later)) }, { actions.onMoveSelected(1) })
                }
                if (count == 1) DropdownMenuItem({ Text(stringResource(R.string.album_set_cover)) }, { menu = false; actions.onCoverSelected() })
                DropdownMenuItem({ Text(stringResource(R.string.album_remove_items)) }, { menu = false; actions.onRemoveSelected() })
            }
        }
    }
}
