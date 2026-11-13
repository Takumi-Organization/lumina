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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.adrian.lumina.R
import com.adrian.lumina.ui.albums.AlbumDetailActions
import com.adrian.lumina.ui.albums.AlbumDetailScreen
import com.adrian.lumina.ui.albums.AlbumFormDialog
import com.adrian.lumina.ui.albums.AlbumPickerDialog
import com.adrian.lumina.ui.albums.AlbumsScreen
import com.adrian.lumina.ui.albums.AlbumsViewModel
import com.adrian.lumina.ui.albums.ConfirmDialog
import com.adrian.lumina.ui.editor.EditorScreen
import com.adrian.lumina.ui.editor.EditorViewModel
import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.TimelineEntry
import com.adrian.lumina.ui.components.LocalSharedScopes
import com.adrian.lumina.ui.components.SharedScopes
import com.adrian.lumina.ui.gallery.GalleryActions
import com.adrian.lumina.ui.gallery.GalleryScreen
import com.adrian.lumina.ui.gallery.GalleryUiState
import com.adrian.lumina.ui.gallery.GalleryViewModel
import com.adrian.lumina.ui.viewer.ViewerScreen

enum class Section { LIBRARY, ALBUMS }

private sealed interface AlbumDialog {
    data class New(val ids: Set<Long>) : AlbumDialog
    data object Edit : AlbumDialog
    data object Delete : AlbumDialog
    data class Pick(val ids: Set<Long>) : AlbumDialog
}

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
    val albumsVm: AlbumsViewModel = viewModel(
        factory = viewModelFactory { initializer { AlbumsViewModel(container.media, container.favorites, container.albums) } }
    )
    val manualAlbums by albumsVm.manualAlbums.collectAsStateWithLifecycle()
    val overview by albumsVm.overview.collectAsStateWithLifecycle()
    val showHidden by albumsVm.showHidden.collectAsStateWithLifecycle()
    val albumDetail by albumsVm.detail.collectAsStateWithLifecycle()
    val albumSelection by albumsVm.selection.collectAsStateWithLifecycle()
    val albumSort by albumsVm.sort.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf(Section.LIBRARY) }
    var editing by remember { mutableStateOf<MediaItem?>(null) }
    var dialog by remember { mutableStateOf<AlbumDialog?>(null) }

    val timeline = (state as? GalleryUiState.Content)?.timeline
    val galleryItems: List<MediaItem> = remember(timeline) {
        timeline?.entries?.filterIsInstance<TimelineEntry.Cell>()?.map { it.item }.orEmpty()
    }
    // El visor recorre lo que se está viendo: el álbum abierto o, si no, la biblioteca.
    val inAlbum = section == Section.ALBUMS && albumDetail != null
    val items: List<MediaItem> = if (inAlbum) albumDetail?.items.orEmpty() else galleryItems
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
        onAddToAlbum = { dialog = AlbumDialog.Pick(selection) },
    )

    when (val d = dialog) {
        is AlbumDialog.New -> AlbumFormDialog("", "", { dialog = null }) { n, desc -> albumsVm.create(n, desc, d.ids); vm.clearSelection(); dialog = null }
        AlbumDialog.Edit -> albumDetail?.album?.let { a ->
            AlbumFormDialog(a.name, a.description, { dialog = null }) { n, desc -> albumsVm.edit(a.id, n, desc); dialog = null }
        }
        AlbumDialog.Delete -> albumDetail?.album?.let { a ->
            ConfirmDialog(stringResource(R.string.album_delete), stringResource(R.string.album_delete_body), { dialog = null }) { albumsVm.deleteAlbum(a.id); dialog = null }
        }
        is AlbumDialog.Pick -> AlbumPickerDialog(
            manualAlbums, { dialog = null },
            onPick = { albumsVm.addTo(it, d.ids); vm.clearSelection(); dialog = null },
            onNew = { dialog = AlbumDialog.New(d.ids) },
        )
        null -> {}
    }

    Box(Modifier.fillMaxSize()) {
    SharedTransitionLayout {
        AnimatedContent(
            targetState = viewerId != null,
            transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(240)) },
            label = "viewer",
        ) { open ->
            CompositionLocalProvider(LocalSharedScopes provides SharedScopes(this@SharedTransitionLayout, this@AnimatedContent)) {
                if (!open) {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))) {
                            when {
                                section == Section.LIBRARY -> GalleryScreen(
                                    state = state, selection = selection, columns = columns, favoritesOnly = favoritesOnly,
                                    allSelectedFavorite = selection.isNotEmpty() && vm.allSelectedAreFavorite(),
                                    actions = actions, gridState = gridState,
                                )
                                albumDetail != null -> AlbumDetailScreen(
                                    detail = albumDetail!!, selection = albumSelection, sort = albumSort,
                                    actions = AlbumDetailActions(
                                        onBack = albumsVm::closeAlbum, onOpenItem = vm::openViewer, onToggle = albumsVm::toggle,
                                        onClearSelection = albumsVm::clearSelection, onSort = albumsVm::setSort,
                                        onRemoveSelected = albumsVm::removeSelected, onCoverSelected = albumsVm::setCoverFromSelection,
                                        onMoveSelected = albumsVm::moveSelected,
                                        onEdit = { dialog = AlbumDialog.Edit },
                                        onToggleFavorite = { albumDetail?.album?.let { albumsVm.setFavorite(it.id, !it.isFavorite) } },
                                        onToggleHidden = { albumDetail?.album?.let { albumsVm.setHidden(it.id, !it.isHidden) } },
                                        onDelete = { dialog = AlbumDialog.Delete },
                                    ),
                                )
                                else -> AlbumsScreen(
                                    overview = overview, showHidden = showHidden, onOpen = albumsVm::openAlbum,
                                    onCreate = { dialog = AlbumDialog.New(emptySet()) }, onShowHidden = albumsVm::setShowHidden,
                                )
                            }
                        }
                        if (selection.isEmpty() && albumDetail == null) NavigationBar {
                            NavigationBarItem(
                                selected = section == Section.LIBRARY, onClick = { section = Section.LIBRARY },
                                icon = { Icon(Icons.Rounded.PhotoLibrary, null) }, label = { Text(stringResource(R.string.tab_library)) },
                                modifier = Modifier.testTag("tab-library"),
                            )
                            NavigationBarItem(
                                selected = section == Section.ALBUMS, onClick = { section = Section.ALBUMS },
                                icon = { Icon(Icons.Rounded.PhotoAlbum, null) }, label = { Text(stringResource(R.string.tab_albums)) },
                                modifier = Modifier.testTag("tab-albums"),
                            )
                        }
                    }
                } else {
                    ViewerScreen(
                        items = items,
                        startId = viewerId ?: -1L,
                        onClose = vm::closeViewer,
                        onPageSettled = { id ->
                            vm.openViewer(id)
                            if (inAlbum) return@ViewerScreen
                            // Al volver, la celda de destino debe estar visible para animar hacia ella.
                            val idx = entryIndexById[id]
                            if (idx != null && gridState.layoutInfo.visibleItemsInfo.none { it.index == idx }) {
                                gridState.requestScrollToItem(idx)
                            }
                        },
                        onToggleFavorite = vm::toggleFavorite,
                        onShare = { share(listOf(it)) },
                        onDelete = { trash(listOf(it)) },
                        onEdit = { editing = it },
                        locationGranted = locationGranted,
                        onRequestLocation = { locationLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION) },
                        loadExif = container.exif::read,
                    )
                }
            }
        }
    }
    editing?.let { item ->
        val editorVm: EditorViewModel = viewModel(
            key = "editor-${item.id}",
            factory = viewModelFactory { initializer { EditorViewModel(item, container.images) } },
        )
        EditorScreen(editorVm, onClose = { editing = null })
    }
    }
}
