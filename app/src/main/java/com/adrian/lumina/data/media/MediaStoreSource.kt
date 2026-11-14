package com.adrian.lumina.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import com.adrian.lumina.domain.model.LibraryStatus
import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.MediaKind
import com.adrian.lumina.domain.model.kindFromMime
import com.adrian.lumina.domain.repository.MediaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext

typealias Querier = (uri: Uri, projection: Array<String>, selection: String?, args: Array<String>?) -> Cursor?

/**
 * Lee imágenes y vídeos de MediaStore (solo metadatos) y observa cambios.
 *
 * Estrategia (de la más estándar a la más permisiva), parando en la primera que devuelve filas:
 * 1. Colecciones de Imágenes y Vídeo del volumen `external` (todos los volúmenes visibles).
 * 2. Lo mismo en `external_primary` (por si un fabricante no expone la vista fusionada).
 * 3. La colección genérica de Archivos, incluyendo filas sin clasificar cuyo MIME es de imagen o de vídeo.
 * Cualquier excepción se registra y queda en [status]; la lista sale vacía pero la UI puede explicar por qué.
 */
class MediaStoreSource(
    private val resolver: ContentResolver,
    private val warmer: ThumbnailWarmer? = null,
    private val querier: Querier = { uri, proj, sel, args -> resolver.query(uri, proj, sel, args, null) },
) : MediaRepository {

    private val _status = MutableStateFlow(LibraryStatus())
    override val status: StateFlow<LibraryStatus> = _status

    private val manualRefresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override fun refresh() { manualRefresh.tryEmit(Unit) }

    /** Una sola lectura compartida por todas las pantallas (antes cada ViewModel consultaba MediaStore por su cuenta). */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val shared: Flow<List<MediaItem>> by lazy { buildFlow().shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1) }

    override fun observeAll(): Flow<List<MediaItem>> = shared

    private fun buildFlow(): Flow<List<MediaItem>> {
        val changes = callbackFlow {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) { trySend(Unit) }
            }
            // Una sola suscripción a toda la autoridad de MediaStore: cubre Imágenes, Vídeo, Archivos y cualquier volumen.
            resolver.registerContentObserver(MediaStore.AUTHORITY_URI, true, observer)
            trySend(Unit)
            awaitClose { resolver.unregisterContentObserver(observer) }
        }
        return merge(changes, manualRefresh).conflate().map { withContext(Dispatchers.IO) { loadSafely() } }
    }

    private fun loadSafely(): List<MediaItem> = try {
        load()
    } catch (t: Throwable) {
        Log.e(TAG, "La lectura de MediaStore falló", t)
        _status.value = LibraryStatus(loaded = true, error = "${t.javaClass.simpleName}: ${t.message}", detail = "excepción")
        emptyList()
    }

    private fun load(): List<MediaItem> {
        val notes = ArrayList<String>()
        var items = ArrayList<MediaItem>()
        for (volume in listOf(MediaStore.VOLUME_EXTERNAL, MediaStore.VOLUME_EXTERNAL_PRIMARY)) {
            val images = queryCollection(MediaStore.Images.Media.getContentUri(volume), MediaKind.IMAGE, "$volume/imágenes", notes)
            val videos = queryCollection(MediaStore.Video.Media.getContentUri(volume), MediaKind.VIDEO, "$volume/vídeos", notes)
            items = ArrayList(images + videos)
            if (items.isNotEmpty()) break
        }
        if (items.isEmpty()) {
            items = ArrayList(queryFiles(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL), notes))
        }
        items.sortByDescending { it.dateMillis }
        val detail = notes.joinToString("; ")
        Log.i(TAG, "MediaStore: ${items.size} elementos ($detail)")
        _status.value = LibraryStatus(loaded = true, error = null, detail = detail)
        warmer?.warm(items)
        return items
    }

    private fun queryCollection(base: Uri, kind: MediaKind, label: String, notes: MutableList<String>): List<MediaItem> =
        try {
            val out = readRows(base, null, null, kind)
            notes += "$label=${out.size}"
            out
        } catch (t: Throwable) {
            Log.w(TAG, "Consulta fallida en $label", t)
            notes += "$label=error(${t.javaClass.simpleName})"
            if (t is SecurityException) throw t // sin permiso: no tiene sentido seguir
            emptyList()
        }

    /** Último recurso: la colección de Archivos, aceptando filas sin clasificar cuyo MIME es de imagen o de vídeo. */
    private fun queryFiles(base: Uri, notes: MutableList<String>): List<MediaItem> = try {
        val t = MediaStore.Files.FileColumns.MEDIA_TYPE
        val m = MediaStore.Files.FileColumns.MIME_TYPE
        val selection = "$t IN (?, ?) OR $m LIKE 'image/%' OR $m LIKE 'video/%'"
        val args = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(), MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
        val out = readRows(base, selection, args, null)
        notes += "archivos=${out.size}"
        out
    } catch (t: Throwable) {
        Log.w(TAG, "Consulta fallida en la colección de archivos", t)
        notes += "archivos=error(${t.javaClass.simpleName})"
        if (t is SecurityException) throw t
        emptyList()
    }

    /** [forcedKind] null = decidir por MEDIA_TYPE y, si no, por MIME (solo en la colección de Archivos). */
    private fun readRows(base: Uri, selection: String?, args: Array<String>?, forcedKind: MediaKind?): List<MediaItem> {
        val full = arrayOf(
            MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_TAKEN, MediaStore.MediaColumns.DATE_ADDED, MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.WIDTH, MediaStore.MediaColumns.HEIGHT, MediaStore.MediaColumns.DURATION,
            MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.BUCKET_DISPLAY_NAME, MediaStore.MediaColumns.RELATIVE_PATH,
        ) + (if (forcedKind == null) arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE) else emptyArray())
        // Mínimo imprescindible por si una versión de MediaStore rechaza alguna columna (IllegalArgumentException).
        val minimal = arrayOf(
            MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_ADDED, MediaStore.MediaColumns.DATE_MODIFIED, MediaStore.MediaColumns.SIZE,
        ) + (if (forcedKind == null) arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE) else emptyArray())
        val cursor = try {
            querier(base, full, selection, args)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Proyección completa rechazada en $base; se reintenta con columnas mínimas", e)
            querier(base, minimal, selection, args)
        }
        if (cursor == null) { Log.w(TAG, "El cursor de $base es null (proveedor no disponible o sin permiso)"); return emptyList() }
        return cursor.use { parse(it, forcedKind) }
    }

    private fun parse(c: Cursor, forced: MediaKind?): List<MediaItem> {
        fun idx(name: String) = c.getColumnIndex(name) // tolerante: una columna ausente no tumba toda la lectura
        fun long(i: Int) = if (i >= 0 && !c.isNull(i)) c.getLong(i) else 0L
        fun int(i: Int) = if (i >= 0 && !c.isNull(i)) c.getInt(i) else 0
        fun str(i: Int) = if (i >= 0) c.getString(i).orEmpty() else ""
        val iId = idx(MediaStore.MediaColumns._ID); val iName = idx(MediaStore.MediaColumns.DISPLAY_NAME)
        val iType = idx(MediaStore.Files.FileColumns.MEDIA_TYPE); val iMime = idx(MediaStore.MediaColumns.MIME_TYPE)
        val iDate = idx(MediaStore.MediaColumns.DATE_TAKEN); val iAdded = idx(MediaStore.MediaColumns.DATE_ADDED)
        val iMod = idx(MediaStore.MediaColumns.DATE_MODIFIED); val iW = idx(MediaStore.MediaColumns.WIDTH)
        val iH = idx(MediaStore.MediaColumns.HEIGHT); val iDur = idx(MediaStore.MediaColumns.DURATION)
        val iSize = idx(MediaStore.MediaColumns.SIZE); val iBucket = idx(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
        val iPath = idx(MediaStore.MediaColumns.RELATIVE_PATH)
        val out = ArrayList<MediaItem>(c.count.coerceAtLeast(0))
        while (c.moveToNext()) {
            val id = long(iId)
            val kind = forced
                ?: when (int(iType)) {
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> MediaKind.VIDEO
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> MediaKind.IMAGE
                    else -> kindFromMime(str(iMime))
                } ?: continue
            val base = if (kind == MediaKind.VIDEO) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            out += MediaItem(
                id = id,
                uri = ContentUris.withAppendedId(base, id).toString(),
                name = str(iName),
                kind = kind,
                // Prioridad: fecha de captura → modificación del archivo → alta en MediaStore.
                dateMillis = long(iDate).takeIf { it > 0 } ?: (long(iMod) * 1000).takeIf { it > 0 } ?: (long(iAdded) * 1000),
                width = int(iW), height = int(iH), durationMs = long(iDur), sizeBytes = long(iSize),
                bucketName = str(iBucket), relativePath = str(iPath),
                isFavorite = false, // los favoritos son de Lumina (FavoritesRepository)
            )
        }
        return out
    }

    private companion object { const val TAG = "LuminaMedia" }
}
