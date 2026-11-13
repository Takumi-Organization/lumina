package com.adrian.lumina.ui.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.adrian.lumina.R
import com.adrian.lumina.data.media.Thumb
import com.adrian.lumina.domain.model.AlbumRef
import com.adrian.lumina.domain.model.SmartType
import com.adrian.lumina.domain.usecase.AlbumCard
import com.adrian.lumina.domain.usecase.AlbumOverview

fun smartTitleRes(t: SmartType): Int = when (t) {
    SmartType.RECENT -> R.string.smart_recent
    SmartType.CAMERA -> R.string.smart_camera
    SmartType.SCREENSHOTS -> R.string.smart_screenshots
    SmartType.DOWNLOADS -> R.string.smart_downloads
    SmartType.PHOTOS -> R.string.smart_photos
    SmartType.VIDEOS -> R.string.smart_videos
    SmartType.FAVORITES -> R.string.smart_favorites
}

@Composable
fun cardTitle(card: AlbumCard): String = (card.ref as? AlbumRef.Smart)?.let { stringResource(smartTitleRes(it.type)) } ?: card.name

/** Lista de álbumes: manuales, automáticos y carpetas del dispositivo. Sin estado propio. */
@Composable
fun AlbumsScreen(
    overview: AlbumOverview,
    showHidden: Boolean,
    onOpen: (AlbumRef) -> Unit,
    onCreate: () -> Unit,
    onShowHidden: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 84.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            section(R.string.albums_mine, overview.manual, onOpen, emptyHint = R.string.albums_none)
            section(R.string.albums_smart, overview.smart, onOpen)
            section(R.string.albums_folders, overview.folders, onOpen)
        }
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
                .statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.tab_albums), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { onShowHidden(!showHidden) }, modifier = Modifier.testTag("albums-show-hidden")) {
                Icon(
                    if (showHidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    stringResource(if (showHidden) R.string.albums_hide_hidden else R.string.albums_show_hidden),
                    tint = if (showHidden) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onCreate, modifier = Modifier.testTag("albums-create")) {
                Icon(Icons.Rounded.Add, stringResource(R.string.album_new), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.section(
    title: Int, cards: List<AlbumCard>, onOpen: (AlbumRef) -> Unit, emptyHint: Int? = null,
) {
    if (cards.isEmpty() && emptyHint == null) return
    item(span = { GridItemSpan(maxLineSpan) }, key = "h-$title") {
        Text(stringResource(title), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (cards.isEmpty() && emptyHint != null) item(span = { GridItemSpan(maxLineSpan) }, key = "e-$title") {
        Text(stringResource(emptyHint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    items(cards, key = { "${title}-${it.ref}" }) { AlbumTile(it) { onOpen(it.ref) } }
}

@Composable
private fun AlbumTile(card: AlbumCard, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick).testTag("album-${card.ref}")) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            card.cover?.let {
                AsyncImage(Thumb(it.uri, com.adrian.lumina.ui.theme.LocalThumbSize.current), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            if (card.isFavorite) Icon(
                Icons.Rounded.Favorite, stringResource(R.string.favorite), tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(16.dp),
            )
        }
        Text(cardTitle(card), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        Text(
            pluralStringResource(R.plurals.items_count, card.count, card.count),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
