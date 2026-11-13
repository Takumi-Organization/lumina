package com.adrian.lumina.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adrian.lumina.domain.model.Timeline
import com.adrian.lumina.domain.repository.MediaRepository
import com.adrian.lumina.domain.usecase.buildTimeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

sealed interface GalleryUiState {
    data object Loading : GalleryUiState
    data object Empty : GalleryUiState
    data class Content(val timeline: Timeline, val total: Int) : GalleryUiState
}

class GalleryViewModel(repo: MediaRepository) : ViewModel() {
    val state: StateFlow<GalleryUiState> = repo.observeAll()
        .map { items ->
            if (items.isEmpty()) GalleryUiState.Empty
            else GalleryUiState.Content(buildTimeline(items), items.size)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GalleryUiState.Loading)
}
