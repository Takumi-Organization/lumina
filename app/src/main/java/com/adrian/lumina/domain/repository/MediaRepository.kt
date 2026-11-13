package com.adrian.lumina.domain.repository

import com.adrian.lumina.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    /** Emite la lista completa y vuelve a emitir cuando MediaStore cambia. */
    fun observeAll(): Flow<List<MediaItem>>
}
