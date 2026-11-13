package com.adrian.lumina

import com.adrian.lumina.domain.model.*
import com.adrian.lumina.domain.repository.AlbumRepository
import com.adrian.lumina.domain.repository.FavoritesRepository
import com.adrian.lumina.domain.repository.MediaRepository
import com.adrian.lumina.ui.albums.AlbumsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeMedia(private val items: List<MediaItem>) : MediaRepository {
    override fun observeAll(): Flow<List<MediaItem>> = MutableStateFlow(items)
}
private class FakeFavs : FavoritesRepository {
    override fun observeIds(): Flow<Set<Long>> = MutableStateFlow(emptySet())
    override suspend fun setFavorite(ids: Collection<Long>, favorite: Boolean) {}
}
/** Álbumes en memoria con la misma semántica que Room (añadir al final, ignorar duplicados). */
private class FakeAlbums : AlbumRepository {
    val state = MutableStateFlow<List<Album>>(emptyList())
    private var next = 1L
    private fun mod(id: Long, f: (Album) -> Album) { state.value = state.value.map { if (it.id == id) f(it) else it } }
    override fun observeAlbums(): Flow<List<Album>> = state
    override suspend fun create(name: String, description: String): Long { val id = next++; state.value = state.value + Album(id, name, description, null, false, false, emptyList()); return id }
    override suspend fun update(id: Long, name: String, description: String) = mod(id) { it.copy(name = name, description = description) }
    override suspend fun setFavorite(id: Long, favorite: Boolean) = mod(id) { it.copy(isFavorite = favorite) }
    override suspend fun setHidden(id: Long, hidden: Boolean) = mod(id) { it.copy(isHidden = hidden) }
    override suspend fun setCover(id: Long, mediaId: Long?) = mod(id) { it.copy(coverMediaId = mediaId) }
    override suspend fun delete(id: Long) { state.value = state.value.filterNot { it.id == id } }
    override suspend fun addItems(id: Long, mediaIds: Collection<Long>) = mod(id) { a -> a.copy(mediaIds = a.mediaIds + mediaIds.filter { it !in a.mediaIds }) }
    override suspend fun removeItems(id: Long, mediaIds: Collection<Long>) = mod(id) { it.copy(mediaIds = it.mediaIds - mediaIds.toSet()) }
    override suspend fun reorder(id: Long, orderedMediaIds: List<Long>) = mod(id) { it.copy(mediaIds = orderedMediaIds) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumsViewModelTest {
    private val now = 1_700_000_000_000L
    private val media = (1L..5L).map { MediaItem(it, "u$it", "$it.jpg", MediaKind.IMAGE, now - it, 1, 1, 0, 1, "b", "DCIM/Camera/", false) }
    private val albums = FakeAlbums()
    private lateinit var vm: AlbumsViewModel

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()); vm = AlbumsViewModel(FakeMedia(media), FakeFavs(), albums) { now } }
    @After fun tearDown() = Dispatchers.resetMain()

    /** Espera (hilos reales: el VM usa flowOn(Default)) a que el detalle cumpla la condición. */
    private fun TestScope.awaitDetail(cond: (com.adrian.lumina.ui.albums.AlbumDetail?) -> Boolean) = runBlockingReal {
        withTimeout(5_000) { vm.detail.first(cond) }
    }
    private fun <T> runBlockingReal(block: suspend () -> T): T = kotlinx.coroutines.runBlocking { block() }

    @Test fun createAddOpenReorderCoverHideDelete() = runTest {
        vm.create("Viaje", "verano", thenAdd = listOf(1, 2, 3))
        runBlockingReal { withTimeout(5_000) { albums.state.first { it.firstOrNull()?.mediaIds == listOf(1L, 2L, 3L) } } }

        vm.openAlbum(AlbumRef.Manual(1))
        awaitDetail { it?.items?.map { i -> i.id } == listOf(1L, 2L, 3L) }

        // Reordenar: mover el 2 una posición después → 1, 3, 2
        vm.toggle(2)
        vm.moveSelected(1)
        runBlockingReal { withTimeout(5_000) { albums.state.first { it.first().mediaIds == listOf(1L, 3L, 2L) } } }
        awaitDetail { it?.items?.map { i -> i.id } == listOf(1L, 3L, 2L) }

        // Con orden ≠ Manual no se reordena
        vm.setSort(AlbumSort.NAME)
        vm.moveSelected(-1)
        assertEquals(listOf(1L, 3L, 2L), albums.state.value.first().mediaIds)
        vm.setSort(AlbumSort.MANUAL)

        // Portada: solo con una seleccionada
        vm.setCoverFromSelection()
        runBlockingReal { withTimeout(5_000) { albums.state.first { it.first().coverMediaId == 2L } } }
        assertTrue(vm.selection.value.isEmpty())

        // Quitar de la selección
        vm.toggle(3); vm.removeSelected()
        runBlockingReal { withTimeout(5_000) { albums.state.first { it.first().mediaIds == listOf(1L, 2L) } } }

        // Ocultar: sale de la lista salvo con «mostrar ocultos»
        vm.closeAlbum()
        vm.setHidden(1, true)
        runBlockingReal { withTimeout(5_000) { vm.overview.first { it.manual.isEmpty() } } }
        vm.setShowHidden(true)
        runBlockingReal { withTimeout(5_000) { vm.overview.first { it.manual.size == 1 && it.manual[0].isHidden } } }

        // Eliminar el álbum abierto lo cierra
        vm.openAlbum(AlbumRef.Manual(1))
        vm.deleteAlbum(1)
        runBlockingReal { withTimeout(5_000) { albums.state.first { it.isEmpty() } } }
        assertEquals(null, vm.open.value)
    }

    @Test fun reorderKeepsIdsMissingFromMediaStoreAtTheEnd() = runTest {
        // El 99 ya no existe en MediaStore: sigue en el álbum y no se pierde al reordenar.
        vm.create("A", thenAdd = listOf(1, 99, 2))
        runBlockingReal { withTimeout(5_000) { albums.state.first { it.firstOrNull()?.mediaIds == listOf(1L, 99L, 2L) } } }
        vm.openAlbum(AlbumRef.Manual(1))
        awaitDetail { it?.items?.map { i -> i.id } == listOf(1L, 2L) }
        vm.toggle(2); vm.moveSelected(-1)
        runBlockingReal { withTimeout(5_000) { albums.state.first { it.first().mediaIds == listOf(2L, 1L, 99L) } } }
    }

    @Test fun blankNameDoesNotCreate() = runTest {
        vm.create("   ")
        assertTrue(albums.state.value.isEmpty())
    }
}
