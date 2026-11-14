package com.adrian.lumina.ui.viewer

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import coil3.SingletonImageLoader
import coil3.request.allowHardware
import coil3.toBitmap
import com.adrian.lumina.domain.usecase.backgroundTint
import com.adrian.lumina.domain.usecase.dominantColor
import com.adrian.lumina.domain.usecase.glowTint
import com.adrian.lumina.ui.theme.Harmony
import com.adrian.lumina.ui.theme.Ink
import com.adrian.lumina.ui.theme.LocalAnimationsEnabled
import com.adrian.lumina.ui.theme.LocalHapticsEnabled
import com.adrian.lumina.ui.theme.Springs
import com.adrian.lumina.ui.theme.SquircleShape
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
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
import com.adrian.lumina.ui.components.mediaSharedBounds
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
    onEdit: ((MediaItem) -> Unit)? = null,
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
    // El botón «girar» del reproductor fuerza horizontal: al cerrar el visor hay que devolver la orientación libre.
    val hostContext = LocalContext.current
    DisposableEffect(hostContext) {
        onDispose { (hostContext as? Activity)?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

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

    // Fondo vivo: matiz oscuro y resplandor de la paleta dominante de la foto, con transición de muelle al cambiar de página.
    val tints = rememberDominantTints(current)
    val bg by animateColorAsState(tints.first, Springs.smooth(), label = "viewerBg")
    val glow by animateColorAsState(tints.second, Springs.smooth(), label = "viewerGlow")
    val scope = rememberCoroutineScope()
    val dragY = remember { Animatable(0f) }
    val density = LocalDensity.current
    val dismissPx = with(density) { 150.dp.toPx() }
    val progress = (abs(dragY.value) / (dismissPx * 2.2f)).coerceIn(0f, 1f)
    val animate = LocalAnimationsEnabled.current

    Box(
        modifier.fillMaxSize()
            .drawBehind {
                drawRect(bg.copy(alpha = 1f - progress * 0.92f))
                drawRect(
                    Brush.radialGradient(listOf(glow.copy(alpha = glow.alpha * (1f - progress)), Color.Transparent), center = Offset(size.width / 2f, size.height * 0.3f), radius = size.width * 1.1f),
                )
            }
            // Arrastrar hacia abajo o arriba (sin zoom) cierra el visor: la foto se encoge y el fondo se atenúa.
            .pointerInput(zoomed, current.id) {
                if (zoomed) return@pointerInput
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dy -> change.consume(); scope.launch { dragY.snapTo(dragY.value + dy) } },
                    onDragEnd = {
                        if (abs(dragY.value) > dismissPx) onClose() else scope.launch { dragY.animateTo(0f, if (animate) Springs.bouncy() else snap()) }
                    },
                    onDragCancel = { scope.launch { dragY.animateTo(0f, if (animate) Springs.bouncy() else snap()) } },
                )
            },
    ) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize().testTag("viewer-pager").graphicsLayer {
                translationY = dragY.value
                val sc = 1f - 0.18f * progress
                scaleX = sc; scaleY = sc
            },
            userScrollEnabled = !zoomed,
            key = { items[it].id },
            beyondViewportPageCount = 1,
            pageSpacing = 12.dp,
        ) { page ->
            val item = items[page]
            ViewerPage(
                item = item,
                isCurrent = page == pager.currentPage,
                onTap = { chrome = !chrome },
                onZoomedChange = { if (page == pager.currentPage) zoomed = it },
                autoPlay = autoPlayVideos,
                onPlayerChange = { activePlayer = it },
                modifier = Modifier.graphicsLayer {
                    // Profundidad: las páginas vecinas se alejan, se encogen y se atenúan; el desplazamiento tiene parallax.
                    val frac = (pager.currentPage - page) + pager.currentPageOffsetFraction
                    val a = abs(frac).coerceAtMost(1f)
                    val s2 = 1f - 0.1f * a
                    scaleX = s2; scaleY = s2
                    alpha = 1f - 0.4f * a
                    translationX = frac * size.width * 0.22f
                },
            )
        }

        val chromeAlpha = (1f - progress * 3f).coerceIn(0f, 1f)
        AnimatedVisibility(
            chrome, Modifier.align(Alignment.TopCenter).graphicsLayer { alpha = chromeAlpha },
            enter = fadeIn(Springs.snappy()) + slideInVertically(Springs.snappy()) { -it / 2 },
            exit = fadeOut(Springs.snappy()) + slideOutVertically(Springs.snappy()) { -it / 2 },
        ) { TopBar(current, pager.currentPage + 1, items.size, onClose) }
        AnimatedVisibility(
            chrome, Modifier.align(Alignment.BottomCenter).graphicsLayer { alpha = chromeAlpha },
            enter = fadeIn(Springs.snappy()) + slideInVertically(Springs.snappy()) { it / 2 },
            exit = fadeOut(Springs.snappy()) + slideOutVertically(Springs.snappy()) { it / 2 },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                activePlayer?.takeIf { current.isVideo }?.let { VideoControls(it) }
                BottomBar(
                    favorite = current.isFavorite,
                    onFavorite = { onToggleFavorite(current) },
                    onShare = { onShare(current) },
                    onInfo = { showInfo = true },
                    onDelete = { onDelete(current) },
                    onEdit = if (onEdit != null && !current.isVideo) ({ onEdit(current) }) else null,
                )
            }
        }
    }

    if (showInfo) {
        InfoSheet(current, locationGranted, onRequestLocation, loadExif) { showInfo = false }
    }
}

@Composable
private fun ViewerPage(
    item: MediaItem,
    isCurrent: Boolean,
    onTap: () -> Unit,
    onZoomedChange: (Boolean) -> Unit,
    autoPlay: Boolean,
    onPlayerChange: (Player?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shared = Modifier.mediaSharedBounds(item.id, LocalAnimationsEnabled.current)
    val context = LocalPlatformContext.current

    if (item.isVideo) {
        // Los vídeos no hacen zoom: el doble toque se reserva para saltar ±10 s.
        Box(modifier.fillMaxSize().then(shared)) {
            VideoPage(item, isCurrent, autoPlay, onTap, onPlayerChange)
        }
        return
    }
    ZoomableBox(onTap = onTap, onZoomedChange = onZoomedChange, modifier = modifier) {
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

/** Barra superior flotante: botón de cierre en cápsula translúcida, fecha en el centro y contador. */
@Composable
private fun TopBar(item: MediaItem, index: Int, total: Int, onClose: () -> Unit) {
    val l = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val day = DateFormat.getDateInstance(DateFormat.LONG, l).format(Date(item.dateMillis))
    val time = DateFormat.getTimeInstance(DateFormat.SHORT, l).format(Date(item.dateMillis))
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(Color(0x66000000)).clickable(onClick = onClose).testTag("viewer-close"),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), tint = Color.White) }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(day, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(time, color = Color(0xB3FFFFFF), style = MaterialTheme.typography.labelMedium)
        }
        Text(
            stringResource(R.string.viewer_counter, index, total), color = Color.White, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.clip(CircleShape).background(Color(0x66000000)).padding(horizontal = 12.dp, vertical = 8.dp).testTag("viewer-counter"),
        )
    }
}

/** Barra inferior flotante: cápsula de esquinas continuas, iconos de trazo fino y corazón con rebote. */
@Composable
private fun BottomBar(favorite: Boolean, onFavorite: () -> Unit, onShare: () -> Unit, onInfo: () -> Unit, onDelete: () -> Unit, onEdit: (() -> Unit)? = null) {
    val animate = LocalAnimationsEnabled.current
    val heart by animateFloatAsState(if (favorite) 1.0f else 0.92f, if (animate) Springs.bouncy() else snap(), label = "heart")
    val haptics = LocalHapticFeedback.current
    val hapticsOn = LocalHapticsEnabled.current
    Row(
        Modifier.navigationBarsPadding().padding(bottom = 14.dp)
            .shadow(24.dp, SquircleShape(26.dp)).clip(SquircleShape(26.dp)).background(Color(0xB3101116)).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        IconButton(onClick = onShare, modifier = Modifier.testTag("viewer-share")) {
            Icon(Icons.Outlined.Share, stringResource(R.string.share), tint = Color.White)
        }
        IconButton(
            onClick = { if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.ContextClick); onFavorite() },
            modifier = Modifier.testTag("viewer-favorite"),
        ) {
            Icon(
                if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                stringResource(if (favorite) R.string.remove_from_favorites else R.string.add_to_favorites),
                tint = if (favorite) Harmony.Rose else Color.White, modifier = Modifier.scale(heart),
            )
        }
        if (onEdit != null) IconButton(onClick = onEdit, modifier = Modifier.testTag("viewer-edit")) {
            Icon(Icons.Outlined.Tune, stringResource(R.string.edit), tint = Color.White)
        }
        IconButton(onClick = onInfo, modifier = Modifier.testTag("viewer-info")) {
            Icon(Icons.Outlined.Info, stringResource(R.string.info), tint = Color.White)
        }
        IconButton(onClick = onDelete, modifier = Modifier.testTag("viewer-delete")) {
            Icon(Icons.Outlined.Delete, stringResource(R.string.delete), tint = Color.White)
        }
    }
}

/** Colores del visor para una foto: (fondo oscuro, resplandor), calculados fuera del hilo principal y cacheados. */
@Composable
private fun rememberDominantTints(item: MediaItem): Pair<Color, Color> {
    val context = LocalContext.current
    val fallback = Pair(Ink.Obsidian, Color.Transparent)
    val state = produceState(DominantCache[item.id] ?: fallback, item.id) {
        DominantCache[item.id]?.let { value = it; return@produceState }
        val tints = withContext(Dispatchers.Default) {
            runCatching {
                val result = SingletonImageLoader.get(context).execute(
                    ImageRequest.Builder(context).data(Thumb(item.uri, 96)).size(48, 48).allowHardware(false).build(),
                )
                val bmp = result.image?.toBitmap(48, 48) ?: return@runCatching null
                val px = IntArray(bmp.width * bmp.height).also { bmp.getPixels(it, 0, bmp.width, 0, 0, bmp.width, bmp.height) }
                val dom = dominantColor(px)
                Pair(Color(backgroundTint(dom)), Color(glowTint(dom)).copy(alpha = 0.42f))
            }.getOrNull()
        }
        if (tints != null) { DominantCache[item.id] = tints; value = tints }
    }
    return state.value
}

private object DominantCache {
    private val map = object : android.util.LruCache<Long, Pair<Color, Color>>(300) {}
    operator fun get(id: Long): Pair<Color, Color>? = map.get(id)
    operator fun set(id: Long, v: Pair<Color, Color>) { map.put(id, v) }
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
