package com.adrian.lumina.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import com.adrian.lumina.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "favorite")
data class FavoriteEntity(@PrimaryKey val mediaId: Long)

@Dao
interface FavoriteDao {
    @Query("SELECT mediaId FROM favorite")
    fun observeIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(items: List<FavoriteEntity>)

    @Query("DELETE FROM favorite WHERE mediaId IN (:ids)")
    suspend fun delete(ids: List<Long>)
}

@Database(entities = [FavoriteEntity::class, AlbumEntity::class, AlbumItemEntity::class], version = 2, exportSchema = true)
abstract class LuminaDatabase : RoomDatabase() {
    abstract fun favorites(): FavoriteDao
    abstract fun albums(): AlbumDao
}

class RoomFavorites(private val dao: FavoriteDao) : FavoritesRepository {
    override fun observeIds(): Flow<Set<Long>> = dao.observeIds().map { it.toSet() }

    override suspend fun setFavorite(ids: Collection<Long>, favorite: Boolean) {
        // SQLite limita las variables por consulta: por lotes.
        ids.chunked(500).forEach { chunk ->
            if (favorite) dao.insert(chunk.map(::FavoriteEntity)) else dao.delete(chunk)
        }
    }
}
