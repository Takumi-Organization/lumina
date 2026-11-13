package com.adrian.lumina.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.Timeline
import com.adrian.lumina.domain.repository.FavoritesRepository
import com.adrian.lumina.domain.repository.MediaRepository
import com.adrian.lumina.domain.usecase.buildTimeline
import com.adrian.lumina.domain.usecase.toggleGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface GalleryUiState {
    data object Loading : GalleryUiState
    data class Empty(val favoritesOnly: Boolean) : GalleryUiState
    data class Content(val timeline: Timeline, val total: Int) : GalleryUiState
}

class GalleryViewModel(
    media: MediaRepository,
    private val favorites: FavoritesRepository,
) : ViewModel() {

    val favoritesOnly = MutableStateFlow(false)
    val columns = MutableStateFlow(DEFAULT_COLUMNS)
    val selection = MutableStateFlow<Set<Long>>(emptySet())

    /** Id del elemento abierto en el visor (null = cuadrícula). Se actualiza al cambiar de página. */
    val viewerId = MutableStateFlow<Long?>(null)
    fun openViewer(id: Long) { viewerId.value = id }
    fun closeViewer() { viewerId.value = null }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch { favorites.setFavorite(listOf(item.id), !item.isFavorite) }
    }

    /** Todos los medios con el indicador de favorito de Lumina ya aplicado. */
    private val items: StateFlow<List<MediaItem>?> =
        combine(media.observeAll(), favorites.observeIds()) { all, favs ->
            all.map { if (it.id in favs) it.copy(isFavorite = true) else it }
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val state: StateFlow<GalleryUiState> = combine(items, favoritesOnly) { all, onlyFav ->
        when {
            all == null -> GalleryUiState.Loading
            else -> {
                val shown = if (onlyFav) all.filter { it.isFavorite } else all
                if (shown.isEmpty()) GalleryUiState.Empty(onlyFav)
                else GalleryUiState.Content(buildTimeline(shown), shown.size)
            }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GalleryUiState.Loading)

    init {
        // Si un elemento desaparece (borrado fuera de Lumina), sale de la selección.
        viewModelScope.launch {
            items.collect { all ->
                if (all != null) {
                    val ids = all.mapTo(HashSet()) { it.id }
                    selection.update { sel -> sel.filterTo(HashSet()) { it in ids } }
                }
            }
        }
    }

    fun toggle(id: Long) = selection.update { if (id in it) it - id else it + id }
    fun toggleGroup(ids: Collection<Long>) = selection.update { it.toggleGroup(ids) }
    fun clearSelection() { selection.value = emptySet() }
    fun setFavoritesOnly(value: Boolean) { favoritesOnly.value = value; clearSelection() }
    fun setColumns(value: Int) { columns.value = value.coerceIn(MIN_COLUMNS, MAX_COLUMNS) }

    /** Si todo lo seleccionado ya es favorito, lo quita; si no, lo marca entero. */
    fun toggleFavoriteOfSelection() {
        val all = items.value ?: return
        val chosen = all.filter { it.id in selection.value }
        if (chosen.isEmpty()) return
        val makeFavorite = !chosen.all { it.isFavorite }
        viewModelScope.launch { favorites.setFavorite(chosen.map { it.id }, makeFavorite) }
    }

    /** True si todos los seleccionados son favoritos (para pintar el corazón lleno). */
    fun allSelectedAreFavorite(): Boolean {
        val all = items.value ?: return false
        val chosen = all.filter { it.id in selection.value }
        return chosen.isNotEmpty() && chosen.all { it.isFavorite }
    }

    companion object {
        const val MIN_COLUMNS = 2
        const val MAX_COLUMNS = 7
        const val DEFAULT_COLUMNS = 4
    }
}
