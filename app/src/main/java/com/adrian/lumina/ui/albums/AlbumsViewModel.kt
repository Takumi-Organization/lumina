package com.adrian.lumina.ui.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adrian.lumina.domain.model.Album
import com.adrian.lumina.domain.model.AlbumRef
import com.adrian.lumina.domain.model.AlbumSort
import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.repository.AlbumRepository
import com.adrian.lumina.domain.repository.FavoritesRepository
import com.adrian.lumina.domain.repository.MediaRepository
import com.adrian.lumina.domain.usecase.AlbumOverview
import com.adrian.lumina.domain.usecase.albumItems
import com.adrian.lumina.domain.usecase.albumOverview
import com.adrian.lumina.domain.usecase.moveIds
import com.adrian.lumina.domain.usecase.sortItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlbumDetail(val ref: AlbumRef, val album: Album?, val items: List<MediaItem>)

class AlbumsViewModel(
    media: MediaRepository,
    favorites: FavoritesRepository,
    private val albums: AlbumRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    val showHidden = MutableStateFlow(false)
    val open = MutableStateFlow<AlbumRef?>(null)
    val sort = MutableStateFlow(AlbumSort.MANUAL)
    val selection = MutableStateFlow<Set<Long>>(emptySet())

    private val allItems: StateFlow<List<MediaItem>> =
        combine(media.observeAll(), favorites.observeIds()) { all, favs ->
            all.map { if (it.id in favs) it.copy(isFavorite = true) else it }
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val manualAlbums: StateFlow<List<Album>> = albums.observeAlbums()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val overview: StateFlow<AlbumOverview> = combine(allItems, manualAlbums, showHidden) { all, al, hidden ->
        albumOverview(all, al, clock(), hidden)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlbumOverview(emptyList(), emptyList(), emptyList()))

    val detail: StateFlow<AlbumDetail?> = combine(open, allItems, manualAlbums, sort) { ref, all, al, s ->
        ref?.let {
            val items = sortItems(albumItems(it, all, al, clock()), if (it is AlbumRef.Manual) s else if (s == AlbumSort.MANUAL) AlbumSort.DATE_DESC else s)
            AlbumDetail(it, (it as? AlbumRef.Manual)?.let { m -> al.firstOrNull { a -> a.id == m.id } }, items)
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun openAlbum(ref: AlbumRef, defaultSort: AlbumSort = AlbumSort.MANUAL) { selection.value = emptySet(); sort.value = defaultSort; open.value = ref }
    fun closeAlbum() { selection.value = emptySet(); open.value = null }
    fun setSort(s: AlbumSort) { sort.value = s }
    fun toggle(id: Long) = selection.update { if (id in it) it - id else it + id }
    fun clearSelection() { selection.value = emptySet() }
    fun setShowHidden(v: Boolean) { showHidden.value = v }

    private fun manualId(): Long? = (open.value as? AlbumRef.Manual)?.id

    fun create(name: String, description: String = "", thenAdd: Collection<Long> = emptyList()) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = albums.create(name, description)
            if (thenAdd.isNotEmpty()) albums.addItems(id, thenAdd)
        }
    }
    fun addTo(albumId: Long, ids: Collection<Long>) { viewModelScope.launch { albums.addItems(albumId, ids) } }
    fun edit(id: Long, name: String, description: String) { if (name.isNotBlank()) viewModelScope.launch { albums.update(id, name, description) } }
    fun setFavorite(id: Long, v: Boolean) { viewModelScope.launch { albums.setFavorite(id, v) } }
    fun setHidden(id: Long, v: Boolean) { viewModelScope.launch { albums.setHidden(id, v) } }
    fun deleteAlbum(id: Long) { viewModelScope.launch { albums.delete(id) }; if (manualId() == id) closeAlbum() }

    fun removeSelected() {
        val id = manualId() ?: return
        val ids = selection.value
        viewModelScope.launch { albums.removeItems(id, ids) }
        clearSelection()
    }

    fun setCoverFromSelection() {
        val id = manualId() ?: return
        val pick = selection.value.singleOrNull() ?: return
        viewModelScope.launch { albums.setCover(id, pick) }
        clearSelection()
    }

    /** Mueve la selección [delta] posiciones dentro del orden manual (solo con orden «Manual»). */
    fun moveSelected(delta: Int) {
        val id = manualId() ?: return
        val album = detail.value?.album ?: return
        if (sort.value != AlbumSort.MANUAL) return
        // Se reordena lo visible; los ids que ya no existen en MediaStore se conservan al final.
        val visible = detail.value?.items?.map { it.id }.orEmpty()
        val moved = moveIds(visible, selection.value, delta)
        if (moved == visible) return
        val gone = album.mediaIds.filter { it !in visible.toSet() }
        viewModelScope.launch { albums.reorder(id, moved + gone) }
    }
}
