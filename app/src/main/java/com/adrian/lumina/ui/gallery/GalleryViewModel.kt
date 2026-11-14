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
    /** [detail]: de dónde se leyó y cuántas filas dio cada fuente (diagnóstico para el usuario). */
    data class Empty(val favoritesOnly: Boolean, val detail: String = "") : GalleryUiState
    /** La lectura de MediaStore falló: se muestra el motivo en vez de una pantalla vacía muda. */
    data class Failed(val message: String, val detail: String = "") : GalleryUiState
    data class Content(val timeline: Timeline, val total: Int) : GalleryUiState
}

class GalleryViewModel(
    private val mediaRepo: MediaRepository,
    private val favorites: FavoritesRepository,
) : ViewModel() {
    private val media = mediaRepo
    fun retry() = media.refresh()

    val favoritesOnly = MutableStateFlow(false)
    val columns = MutableStateFlow(DEFAULT_COLUMNS)
    /** Nivel de zoom: años ⇢ meses ⇢ días (pellizco más allá de la densidad máxima/mínima). */
    val level = MutableStateFlow(com.adrian.lumina.domain.usecase.ZoomLevel.DAYS)
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

    val state: StateFlow<GalleryUiState> = combine(items, favoritesOnly, media.status, columns) { all, onlyFav, status, cols ->
        when {
            all == null -> GalleryUiState.Loading
            all.isEmpty() && status.error != null -> GalleryUiState.Failed(status.error, status.detail)
            else -> {
                val shown = if (onlyFav) all.filter { it.isFavorite } else all
                if (shown.isEmpty()) GalleryUiState.Empty(onlyFav, status.detail)
                else GalleryUiState.Content(buildTimeline(shown, target = densityFor(cols)), shown.size)
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
    /**
     * Pellizco: en días cambia la densidad y, al pasar del límite, sube de nivel (meses → años). En meses y años el
     * pellizco solo cambia de nivel (juntar dedos = más grueso, separar = más fino).
     */
    fun setColumns(value: Int) {
        val z = com.adrian.lumina.domain.usecase.ZoomLevel.entries
        val cur = level.value
        if (cur != com.adrian.lumina.domain.usecase.ZoomLevel.DAYS) {
            level.value = if (value > columns.value) z.getOrElse(cur.ordinal - 1) { cur } else z.getOrElse(cur.ordinal + 1) { cur }
            return
        }
        if (value > MAX_COLUMNS) { level.value = com.adrian.lumina.domain.usecase.ZoomLevel.MONTHS; clearSelection(); return }
        columns.value = value.coerceIn(MIN_COLUMNS, MAX_COLUMNS)
    }
    fun setLevel(l: com.adrian.lumina.domain.usecase.ZoomLevel) { level.value = l }

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
        /** Suma de proporciones por fila del mosaico para un «nivel de zoom» (2 = fotos enormes, 7 = muy denso). */
        fun densityFor(columns: Int) = columns * 0.75f
        const val MIN_COLUMNS = 2
        const val MAX_COLUMNS = 7
        const val DEFAULT_COLUMNS = 4
    }
}
