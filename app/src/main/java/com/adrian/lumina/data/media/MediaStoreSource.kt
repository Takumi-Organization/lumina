package com.adrian.lumina.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.MediaKind
import com.adrian.lumina.domain.repository.MediaRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Lee imágenes y vídeos de MediaStore (solo metadatos) y observa cambios. */
class MediaStoreSource(private val resolver: ContentResolver) : MediaRepository {

    private val filesUri: Uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

    override fun observeAll(): Flow<List<MediaItem>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { trySend(Unit) }
        }
        resolver.registerContentObserver(filesUri, true, observer)
        trySend(Unit)
        awaitClose { resolver.unregisterContentObserver(observer) }
    }.conflate().map { withContext(Dispatchers.IO) { query() } }

    private fun query(): List<MediaItem> {
        val fav = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        val projection = buildList {
            add(MediaStore.Files.FileColumns._ID)
            add(MediaStore.Files.FileColumns.DISPLAY_NAME)
            add(MediaStore.Files.FileColumns.MEDIA_TYPE)
            add(MediaStore.Files.FileColumns.DATE_TAKEN)
            add(MediaStore.Files.FileColumns.DATE_ADDED)
            add(MediaStore.Files.FileColumns.DATE_MODIFIED)
            add(MediaStore.Files.FileColumns.WIDTH)
            add(MediaStore.Files.FileColumns.HEIGHT)
            add(MediaStore.Files.FileColumns.DURATION)
            add(MediaStore.Files.FileColumns.SIZE)
            add(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
            add(MediaStore.Files.FileColumns.RELATIVE_PATH)
            if (fav) add(MediaStore.Files.FileColumns.IS_FAVORITE)
        }.toTypedArray()
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)"
        val args = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
        )
        val out = ArrayList<MediaItem>()
        resolver.query(filesUri, projection, selection, args, null)?.use { c ->
            val iId = c.getColumnIndexOrThrow(projection[0])
            val iName = c.getColumnIndexOrThrow(projection[1])
            val iType = c.getColumnIndexOrThrow(projection[2])
            val iDate = c.getColumnIndexOrThrow(projection[3])
            val iAdded = c.getColumnIndexOrThrow(projection[4])
            val iMod = c.getColumnIndexOrThrow(projection[5])
            val iW = c.getColumnIndexOrThrow(projection[6])
            val iH = c.getColumnIndexOrThrow(projection[7])
            val iDur = c.getColumnIndexOrThrow(projection[8])
            val iSize = c.getColumnIndexOrThrow(projection[9])
            val iBucket = c.getColumnIndexOrThrow(projection[10])
            val iPath = c.getColumnIndexOrThrow(projection[11])
            val iFav = if (fav) c.getColumnIndexOrThrow(projection[12]) else -1
            while (c.moveToNext()) {
                val id = c.getLong(iId)
                val video = c.getInt(iType) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                val base = if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                out += MediaItem(
                    id = id,
                    uri = ContentUris.withAppendedId(base, id).toString(),
                    name = c.getString(iName).orEmpty(),
                    kind = if (video) MediaKind.VIDEO else MediaKind.IMAGE,
                    // Prioridad: fecha de captura → modificación del archivo → alta en MediaStore.
                    dateMillis = c.getLong(iDate).takeIf { it > 0 }
                        ?: (c.getLong(iMod) * 1000).takeIf { it > 0 }
                        ?: (c.getLong(iAdded) * 1000),
                    width = c.getInt(iW),
                    height = c.getInt(iH),
                    durationMs = c.getLong(iDur),
                    sizeBytes = c.getLong(iSize),
                    bucketName = c.getString(iBucket).orEmpty(),
                    relativePath = c.getString(iPath).orEmpty(),
                    isFavorite = iFav >= 0 && c.getInt(iFav) == 1,
                )
            }
        }
        out.sortByDescending { it.dateMillis }
        return out
    }
}
