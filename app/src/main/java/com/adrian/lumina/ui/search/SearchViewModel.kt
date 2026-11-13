package com.adrian.lumina.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.repository.FavoritesRepository
import com.adrian.lumina.domain.repository.MediaRepository
import com.adrian.lumina.domain.repository.SearchProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

sealed interface SearchUi {
    data object Idle : SearchUi
    data class Results(val items: List<MediaItem>) : SearchUi
}

@OptIn(FlowPreview::class)
class SearchViewModel(
    media: MediaRepository,
    favorites: FavoritesRepository,
    private val provider: SearchProvider,
) : ViewModel() {
    val query = MutableStateFlow("")

    private val all: StateFlow<List<MediaItem>> = combine(media.observeAll(), favorites.observeIds()) { m, favs ->
        m.map { if (it.id in favs) it.copy(isFavorite = true) else it }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val state: StateFlow<SearchUi> = combine(query.debounce(150), all) { q, items ->
        if (q.isBlank()) SearchUi.Idle
        else provider.search(q, items).map { it.item }.let { SearchUi.Results(it) }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUi.Idle)

    fun setQuery(q: String) { query.value = q }
}
