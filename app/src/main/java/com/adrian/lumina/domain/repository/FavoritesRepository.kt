package com.adrian.lumina.domain.repository

import kotlinx.coroutines.flow.Flow

/** Favoritos propios de Lumina (no tocan el indicador del sistema, que exige un diálogo por acción). */
interface FavoritesRepository {
    fun observeIds(): Flow<Set<Long>>
    suspend fun setFavorite(ids: Collection<Long>, favorite: Boolean)
}
