package com.adrian.lumina.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.adrian.lumina.domain.model.Album
import com.adrian.lumina.domain.repository.AlbumRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Entity(tableName = "album")
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val coverMediaId: Long? = null,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val createdAt: Long,
)

@Entity(
    tableName = "album_item",
    primaryKeys = ["albumId", "mediaId"],
    foreignKeys = [ForeignKey(AlbumEntity::class, parentColumns = ["id"], childColumns = ["albumId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("mediaId")],
)
data class AlbumItemEntity(val albumId: Long, val mediaId: Long, val position: Int)

@Dao
abstract class AlbumDao {
    @Query("SELECT * FROM album ORDER BY isFavorite DESC, createdAt DESC, id DESC")
    abstract fun observeAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM album_item ORDER BY albumId, position")
    abstract fun observeAllItems(): Flow<List<AlbumItemEntity>>

    @Insert abstract suspend fun insert(album: AlbumEntity): Long

    @Query("UPDATE album SET name = :name, description = :description WHERE id = :id")
    abstract suspend fun update(id: Long, name: String, description: String)

    @Query("UPDATE album SET isFavorite = :value WHERE id = :id") abstract suspend fun setFavorite(id: Long, value: Boolean)
    @Query("UPDATE album SET isHidden = :value WHERE id = :id") abstract suspend fun setHidden(id: Long, value: Boolean)
    @Query("UPDATE album SET coverMediaId = :mediaId WHERE id = :id") abstract suspend fun setCover(id: Long, mediaId: Long?)
    @Query("DELETE FROM album WHERE id = :id") abstract suspend fun delete(id: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM album_item WHERE albumId = :id")
    abstract suspend fun maxPosition(id: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE) abstract suspend fun insertItems(items: List<AlbumItemEntity>)

    @Query("DELETE FROM album_item WHERE albumId = :id AND mediaId IN (:mediaIds)")
    abstract suspend fun deleteItems(id: Long, mediaIds: List<Long>)

    @Query("UPDATE album_item SET position = :position WHERE albumId = :id AND mediaId = :mediaId")
    abstract suspend fun setPosition(id: Long, mediaId: Long, position: Int)

    /** Añade al final; los ya presentes se ignoran (clave primaria). */
    @Transaction
    open suspend fun addItems(id: Long, mediaIds: List<Long>) {
        var p = maxPosition(id) + 1
        insertItems(mediaIds.map { AlbumItemEntity(id, it, p++) })
    }

    @Transaction
    open suspend fun reorder(id: Long, ordered: List<Long>) {
        ordered.forEachIndexed { index, mediaId -> setPosition(id, mediaId, index) }
    }
}

class RoomAlbums(private val dao: AlbumDao, private val now: () -> Long = System::currentTimeMillis) : AlbumRepository {

    override fun observeAlbums(): Flow<List<Album>> = combine(dao.observeAlbums(), dao.observeAllItems()) { albums, items ->
        val byAlbum = items.groupBy { it.albumId }
        albums.map { a ->
            Album(
                a.id, a.name, a.description, a.coverMediaId, a.isFavorite, a.isHidden,
                byAlbum[a.id].orEmpty().sortedBy { it.position }.map { it.mediaId },
            )
        }
    }

    override suspend fun create(name: String, description: String) =
        dao.insert(AlbumEntity(name = name.trim(), description = description.trim(), createdAt = now()))

    override suspend fun update(id: Long, name: String, description: String) = dao.update(id, name.trim(), description.trim())
    override suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)
    override suspend fun setHidden(id: Long, hidden: Boolean) = dao.setHidden(id, hidden)
    override suspend fun setCover(id: Long, mediaId: Long?) = dao.setCover(id, mediaId)
    override suspend fun delete(id: Long) = dao.delete(id)
    override suspend fun addItems(id: Long, mediaIds: Collection<Long>) = dao.addItems(id, mediaIds.distinct())

    override suspend fun removeItems(id: Long, mediaIds: Collection<Long>) {
        mediaIds.chunked(500).forEach { dao.deleteItems(id, it) } // límite de variables de SQLite
    }

    override suspend fun reorder(id: Long, orderedMediaIds: List<Long>) = dao.reorder(id, orderedMediaIds)
}

/** v1 (solo favoritos) → v2 (álbumes). Debe coincidir con schemas/…/2.json: lo comprueba MigrationTest. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `album` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                "`description` TEXT NOT NULL, `coverMediaId` INTEGER, `isFavorite` INTEGER NOT NULL, " +
                "`isHidden` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `album_item` (`albumId` INTEGER NOT NULL, `mediaId` INTEGER NOT NULL, " +
                "`position` INTEGER NOT NULL, PRIMARY KEY(`albumId`, `mediaId`), " +
                "FOREIGN KEY(`albumId`) REFERENCES `album`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_item_mediaId` ON `album_item` (`mediaId`)")
    }
}
