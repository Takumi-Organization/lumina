package com.adrian.lumina.app

import android.content.Context
import androidx.room.Room
import com.adrian.lumina.data.db.LuminaDatabase
import com.adrian.lumina.data.db.RoomFavorites
import com.adrian.lumina.data.media.ExifReader
import com.adrian.lumina.data.media.MediaActions
import com.adrian.lumina.data.media.MediaStoreSource
import com.adrian.lumina.domain.repository.FavoritesRepository
import com.adrian.lumina.domain.repository.MediaRepository

/** Inyección manual: pocas dependencias, sin framework (ver docs/ARQUITECTURA.md). */
class AppContainer(context: Context) {
    private val db = Room.databaseBuilder(context, LuminaDatabase::class.java, "lumina.db").build()
    val media: MediaRepository = MediaStoreSource(context.contentResolver)
    val favorites: FavoritesRepository = RoomFavorites(db.favorites())
    val actions = MediaActions(context)
    val exif = ExifReader(context)
}
