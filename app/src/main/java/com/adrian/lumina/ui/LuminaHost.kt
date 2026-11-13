package com.adrian.lumina.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.adrian.lumina.app.AppContainer
import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.TimelineEntry
import com.adrian.lumina.ui.components.LocalSharedScopes
import com.adrian.lumina.ui.components.SharedScopes
import com.adrian.lumina.ui.gallery.GalleryActions
import com.adrian.lumina.ui.gallery.GalleryScreen
import com.adrian.lumina.ui.gallery.GalleryUiState
import com.adrian.lumina.ui.gallery.GalleryViewModel
import com.adrian.lumina.ui.viewer.ViewerScreen

/** Une cuadrícula y visor en un solo árbol para poder animar la transición compartida. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LuminaHost(container: AppContainer) {
    val vm: GalleryViewModel = viewModel(
        factory = viewModelFactory { initializer { GalleryViewModel(container.media, container.favorites) } }
    )
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val selection by vm.selection.collectAsStateWithLifecycle()
    val columns by vm.columns.collectAsStateWithLifecycle()
    val favoritesOnly by vm.favoritesOnly.collectAsStateWithLifecycle()
    val viewerId by vm.viewerId.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()

    val timeline = (state as? GalleryUiState.Content)?.timeline
    val items: List<MediaItem> = remember(timeline) {
        timeline?.entries?.filterIsInstance<TimelineEntry.Cell>()?.map { it.item }.orEmpty()
    }
    val entryIndexById = remember(timeline) {
        timeline?.entries?.withIndex()?.mapNotNull { (i, e) -> (e as? TimelineEntry.Cell)?.let { it.item.id to i } }?.toMap().orEmpty()
    }

    // El resultado del diálogo de papelera no hace falta leerlo: MediaStore notifica el cambio y la lista se refresca.
    val trashLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}
    fun trash(list: List<MediaItem>) {
        if (list.isEmpty()) return
        runCatching { trashLauncher.launch(IntentSenderRequest.Builder(container.actions.trashRequest(list)).build()) }
            .onFailure { android.util.Log.w("Lumina", "No se pudo pedir la papelera para ${list.size} elemento(s)", it) }
    }
    fun share(list: List<MediaItem>) {
        if (list.isNotEmpty()) runCatching { context.startActivity(container.actions.shareChooser(list)) }
            .onFailure { android.util.Log.w("Lumina", "No se pudo compartir", it) }
    }

    var locationGranted by remember { mutableStateOf(container.exif.hasLocationPermission()) }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { locationGranted = it }

    val actions = GalleryActions(
        onToggle = vm::toggle, onToggleDay = vm::toggleGroup,
        onClearSelection = vm::clearSelection, onFavoriteSelected = vm::toggleFavoriteOfSelection,
        onFavoritesOnly = vm::setFavoritesOnly, onColumns = vm::setColumns,
        onOpen = vm::openViewer,
        onShareSelected = { share(items.filter { it.id in selection }) },
        onDeleteSelected = { trash(items.filter { it.id in selection }) },
    )

    SharedTransitionLayout {
        AnimatedContent(
            targetState = viewerId != null,
            transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(240)) },
            label = "viewer",
        ) { open ->
            CompositionLocalProvider(LocalSharedScopes provides SharedScopes(this@SharedTransitionLayout, this@AnimatedContent)) {
                if (!open) {
                    GalleryScreen(
                        state = state, selection = selection, columns = columns, favoritesOnly = favoritesOnly,
                        allSelectedFavorite = selection.isNotEmpty() && vm.allSelectedAreFavorite(),
                        actions = actions, gridState = gridState,
                    )
                } else {
                    ViewerScreen(
                        items = items,
                        startId = viewerId ?: -1L,
                        onClose = vm::closeViewer,
                        onPageSettled = { id ->
                            vm.openViewer(id)
                            // Al volver, la celda de destino debe estar visible para animar hacia ella.
                            val idx = entryIndexById[id]
                            if (idx != null && gridState.layoutInfo.visibleItemsInfo.none { it.index == idx }) {
                                gridState.requestScrollToItem(idx)
                            }
                        },
                        onToggleFavorite = vm::toggleFavorite,
                        onShare = { share(listOf(it)) },
                        onDelete = { trash(listOf(it)) },
                        onPlayVideo = { runCatching { context.startActivity(container.actions.viewExternally(it)) } },
                        locationGranted = locationGranted,
                        onRequestLocation = { locationLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION) },
                        loadExif = container.exif::read,
                    )
                }
            }
        }
    }
}
