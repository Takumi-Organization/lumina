package com.adrian.lumina.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adrian.lumina.domain.model.AppSettings
import com.adrian.lumina.domain.repository.AlbumRepository
import com.adrian.lumina.domain.repository.FavoritesRepository
import com.adrian.lumina.domain.repository.MediaRepository
import com.adrian.lumina.domain.repository.SettingsRepository
import com.adrian.lumina.domain.usecase.StorageSummary
import com.adrian.lumina.domain.usecase.storageSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StorageUi(val summary: StorageSummary, val favorites: Int, val albums: Int)

class SettingsViewModel(
    private val repo: SettingsRepository,
    media: MediaRepository,
    favorites: FavoritesRepository,
    albums: AlbumRepository,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val storage: StateFlow<StorageUi?> = combine(media.observeAll().map(::storageSummary), favorites.observeIds(), albums.observeAlbums()) { s, f, a ->
        StorageUi(s, f.size, a.size)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun update(transform: (AppSettings) -> AppSettings) { viewModelScope.launch { repo.update(transform) } }
}
