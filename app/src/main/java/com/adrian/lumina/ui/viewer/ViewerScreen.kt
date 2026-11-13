package com.adrian.lumina.ui.viewer

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.size.Size
import com.adrian.lumina.R
import com.adrian.lumina.data.media.Thumb
import com.adrian.lumina.domain.model.ExifInfo
import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.ui.components.LocalSharedScopes
import com.adrian.lumina.ui.components.mediaKey
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay

private const val CHROME_HIDE_MS = 3500L

@Composable
fun ViewerScreen(
    items: List<MediaItem>,
    startId: Long,
    onClose: () -> Unit,
    onPageSettled: (Long) -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onShare: (MediaItem) -> Unit,
    onDelete: (MediaItem) -> Unit,
    autoPlayVideos: Boolean = false,
    autoHideChromeMs: Long = CHROME_HIDE_MS,
    locationGranted: Boolean,
    onRequestLocation: () -> Unit,
    loadExif: suspend (MediaItem) -> ExifInfo?,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) { LaunchedEffect(Unit) { onClose() }; return }

    val pager = rememberPagerState(initialPage = items.indexOfFirst { it.id == startId }.coerceAtLeast(0)) { items.size }
    var zoomed by remember { mutableStateOf(false) }
    var chrome by remember { mutableStateOf(true) }
    var showInfo by rememberSaveable { mutableStateOf(false) }
    val current = items[pager.currentPage.coerceIn(0, items.lastIndex)]
    val settled by rememberUpdatedState(onPageSettled)
    var activePlayer by remember { mutableStateOf<Player?>(null) }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    ImmersiveWhen(landscape)

    BackHandler { if (showInfo) showInfo = false else onClose() }
    LightIconsOnBlack()

    // Los controles se ocultan solos; cualquier cambio de página o toque los recupera.
    LaunchedEffect(chrome, pager.currentPage, showInfo) {
        if (chrome && !showInfo) { delay(autoHideChromeMs); chrome = false }
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { page -> items.getOrNull(page)?.let { settled(it.id) } }
    }
    LaunchedEffect(pager.currentPage) { zoomed = false }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize().testTag("viewer-pager"),
            userScrollEnabled = !zoomed,
            key = { items[it].id },
            beyondViewportPageCount = 1,
        ) { page ->
            val item = items[page]
            ViewerPage(
                item = item,
                isCurrent = page == pager.currentPage,
                onTap = { chrome = !chrome },
                onZoomedChange = { if (page == pager.currentPage) zoomed = it },
                autoPlay = autoPlayVideos,
                onPlayerChange = { activePlayer = it },
            )
        }

        AnimatedVisibility(chrome, Modifier.align(Alignment.TopCenter), enter = fadeIn(), exit = fadeOut()) {
            TopBar(current, pager.currentPage + 1, items.size, onClose)
        }
        AnimatedVisibility(chrome, Modifier.align(Alignment.BottomCenter), enter = fadeIn(), exit = fadeOut()) {
            Column {
                activePlayer?.takeIf { current.isVideo }?.let { VideoControls(it) }
                BottomBar(
                    favorite = current.isFavorite,
                    onFavorite = { onToggleFavorite(current) },
                    onShare = { onShare(current) },
                    onInfo = { showInfo = true },
                    onDelete = { onDelete(current) },
                )
            }
        }
    }

    if (showInfo) {
        InfoSheet(current, locationGranted, onRequestLocation, loadExif) { showInfo = false }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ViewerPage(
    item: MediaItem,
    isCurrent: Boolean,
    onTap: () -> Unit,
    onZoomedChange: (Boolean) -> Unit,
    autoPlay: Boolean,
    onPlayerChange: (Player?) -> Unit,
) {
    val scopes = LocalSharedScopes.current
    val shared = if (scopes != null) with(scopes.shared) {
        Modifier.sharedBounds(
            rememberSharedContentState(mediaKey(item.id)), scopes.visibility,
        )
    } else Modifier
    val context = LocalPlatformContext.current

    if (item.isVideo) {
        // Los vídeos no hacen zoom: el doble toque se reserva para saltar ±10 s.
        Box(Modifier.fillMaxSize().then(shared)) {
            VideoPage(item, isCurrent, autoPlay, onTap, onPlayerChange)
        }
        return
    }
    ZoomableBox(onTap = onTap, onZoomedChange = onZoomedChange) {
        Box(Modifier.fillMaxSize().then(shared)) {
            // Capa 1: la misma miniatura que la cuadrícula (ya en caché) → sin parpadeo al abrir.
            AsyncImage(Thumb(item.uri), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            // Capa 2: imagen real, acotada a 2048 px (suficiente para 4–6× sin cargar el original completo).
            if (isCurrent) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(android.net.Uri.parse(item.uri)).size(Size(2048, 2048)).build(),
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

@Composable
private fun TopBar(item: MediaItem, index: Int, total: Int, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color(0x8C000000)).statusBarsPadding().padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose, modifier = Modifier.testTag("viewer-close")) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = Color.White)
        }
        Text(
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(item.dateMillis)),
            color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(start = 4.dp),
        )
        Text(
            stringResource(R.string.viewer_counter, index, total),
            color = Color(0xFFCFC8BC), style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(end = 16.dp).testTag("viewer-counter"),
        )
    }
}

@Composable
private fun BottomBar(favorite: Boolean, onFavorite: () -> Unit, onShare: () -> Unit, onInfo: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color(0x8C000000)).navigationBarsPadding().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        IconButton(onClick = onShare, modifier = Modifier.testTag("viewer-share")) {
            Icon(Icons.Rounded.Share, stringResource(R.string.share), tint = Color.White)
        }
        IconButton(onClick = onFavorite, modifier = Modifier.testTag("viewer-favorite")) {
            Icon(
                if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                stringResource(if (favorite) R.string.remove_from_favorites else R.string.add_to_favorites),
                tint = if (favorite) MaterialTheme.colorScheme.primary else Color.White,
            )
        }
        IconButton(onClick = onInfo, modifier = Modifier.testTag("viewer-info")) {
            Icon(Icons.Rounded.Info, stringResource(R.string.info), tint = Color.White)
        }
        IconButton(onClick = onDelete, modifier = Modifier.testTag("viewer-delete")) {
            Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = Color.White)
        }
    }
}

/** El visor siempre es negro: los iconos de la barra de estado/navegación deben ser claros. */
@Composable
private fun LightIconsOnBlack() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val prevStatus = controller?.isAppearanceLightStatusBars
        val prevNav = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (prevStatus != null) controller.isAppearanceLightStatusBars = prevStatus
            if (prevNav != null) controller.isAppearanceLightNavigationBars = prevNav
        }
    }
}

/** En horizontal se ocultan las barras del sistema (también al reproducir vídeo); un gesto desde el borde las muestra un momento. */
@Composable
private fun ImmersiveWhen(active: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, active) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        if (active && controller != null) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
}
