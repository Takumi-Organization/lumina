package com.adrian.lumina.domain.repository

import com.adrian.lumina.domain.model.Album
import kotlinx.coroutines.flow.Flow

interface AlbumRepository {
    /** Favoritos primero, luego más recientes; cada álbum con sus ids en orden manual. */
    fun observeAlbums(): Flow<List<Album>>
    suspend fun create(name: String, description: String = ""): Long
    suspend fun update(id: Long, name: String, description: String)
    suspend fun setFavorite(id: Long, favorite: Boolean)
    suspend fun setHidden(id: Long, hidden: Boolean)
    suspend fun setCover(id: Long, mediaId: Long?)
    suspend fun delete(id: Long)
    suspend fun addItems(id: Long, mediaIds: Collection<Long>)
    suspend fun removeItems(id: Long, mediaIds: Collection<Long>)
    suspend fun reorder(id: Long, orderedMediaIds: List<Long>)
}
