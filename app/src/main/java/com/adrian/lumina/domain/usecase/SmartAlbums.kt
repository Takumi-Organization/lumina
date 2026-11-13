package com.adrian.lumina.domain.usecase

import com.adrian.lumina.domain.model.AlbumSort
import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.SmartType

const val RECENT_DAYS = 30L
private const val DAY_MS = 86_400_000L

private fun String.hasSegment(name: String) =
    split('/').any { it.equals(name, ignoreCase = true) }

/** Capturas: carpeta «Screenshots» o nombre «Screenshot…» (algunos fabricantes usan «Captura»). */
fun MediaItem.isScreenshot() = relativePath.hasSegment("Screenshots") ||
    name.startsWith("Screenshot", ignoreCase = true) || name.startsWith("Captura", ignoreCase = true)

fun MediaItem.isCamera() = relativePath.startsWith("DCIM/Camera", ignoreCase = true)
fun MediaItem.isDownload() = relativePath.startsWith("Download", ignoreCase = true)

/** Elementos de una categoría automática, más recientes primero. Solo usa metadatos que MediaStore expone. */
fun smartAlbumItems(type: SmartType, items: List<MediaItem>, nowMillis: Long): List<MediaItem> {
    val pick: (MediaItem) -> Boolean = when (type) {
        SmartType.RECENT -> { i -> i.dateMillis >= nowMillis - RECENT_DAYS * DAY_MS }
        SmartType.CAMERA -> { i -> i.isCamera() }
        SmartType.SCREENSHOTS -> { i -> i.isScreenshot() }
        SmartType.DOWNLOADS -> { i -> i.isDownload() }
        SmartType.PHOTOS -> { i -> !i.isVideo && !i.isScreenshot() }
        SmartType.VIDEOS -> { i -> i.isVideo }
        SmartType.FAVORITES -> { i -> i.isFavorite }
    }
    return items.filter(pick).sortedByDescending { it.dateMillis }
}

data class FolderInfo(val path: String, val name: String, val items: List<MediaItem>)

/** Carpetas del dispositivo (por RELATIVE_PATH), de mayor a menor número de elementos. */
fun folderAlbums(items: List<MediaItem>): List<FolderInfo> =
    items.filter { it.relativePath.isNotBlank() }
        .groupBy { it.relativePath }
        .map { (path, list) ->
            FolderInfo(path, path.trimEnd('/').substringAfterLast('/').ifBlank { path }, list.sortedByDescending { it.dateMillis })
        }
        .sortedWith(compareByDescending<FolderInfo> { it.items.size }.thenBy { it.name.lowercase() })

/** Ordena para visualizar; MANUAL conserva el orden recibido (el del álbum). */
fun sortItems(items: List<MediaItem>, sort: AlbumSort): List<MediaItem> = when (sort) {
    AlbumSort.MANUAL -> items
    AlbumSort.DATE_DESC -> items.sortedByDescending { it.dateMillis }
    AlbumSort.DATE_ASC -> items.sortedBy { it.dateMillis }
    AlbumSort.NAME -> items.sortedBy { it.name.lowercase() }
}

/** Mueve [id] [delta] posiciones (negativo = antes), acotado a los extremos. Devuelve la misma lista si no está. */
fun moveId(ids: List<Long>, id: Long, delta: Int): List<Long> {
    val from = ids.indexOf(id)
    if (from < 0) return ids
    val to = (from + delta).coerceIn(0, ids.lastIndex)
    if (to == from) return ids
    return ids.toMutableList().also { it.removeAt(from); it.add(to, id) }
}

/** Mueve un bloque de ids seleccionados conservando su orden relativo, [delta] posiciones. */
fun moveIds(ids: List<Long>, selected: Set<Long>, delta: Int): List<Long> {
    var out = ids
    val order = if (delta < 0) ids.filter { it in selected } else ids.filter { it in selected }.reversed()
    // Empujar de uno en uno; si el primero/último ya está en el borde, el bloque no se mueve.
    repeat(kotlin.math.abs(delta)) {
        val edgeBlocked = if (delta < 0) out.indexOf(order.first()) == 0 else out.indexOf(order.first()) == out.lastIndex
        if (!edgeBlocked) order.forEach { out = moveId(out, it, if (delta < 0) -1 else 1) }
    }
    return out
}
