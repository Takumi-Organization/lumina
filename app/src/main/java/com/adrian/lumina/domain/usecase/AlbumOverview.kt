package com.adrian.lumina.domain.usecase

import com.adrian.lumina.domain.model.Album
import com.adrian.lumina.domain.model.AlbumRef
import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.SmartType

/** Tarjeta de la lista de álbumes. [name] está vacío en los automáticos: la UI lo traduce según [ref]. */
data class AlbumCard(
    val ref: AlbumRef,
    val name: String,
    val count: Int,
    val cover: MediaItem?,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
)

data class AlbumOverview(val manual: List<AlbumCard>, val smart: List<AlbumCard>, val folders: List<AlbumCard>)

/** Elementos de un álbum manual en su orden; los ids que ya no existen en MediaStore se omiten. */
fun manualAlbumItems(album: Album, all: List<MediaItem>): List<MediaItem> {
    val byId = all.associateBy { it.id }
    return album.mediaIds.mapNotNull { byId[it] }
}

/** Resuelve cualquier referencia (manual, automática o carpeta) a sus elementos. */
fun albumItems(ref: AlbumRef, all: List<MediaItem>, albums: List<Album>, nowMillis: Long): List<MediaItem> = when (ref) {
    is AlbumRef.Manual -> albums.firstOrNull { it.id == ref.id }?.let { manualAlbumItems(it, all) }.orEmpty()
    is AlbumRef.Smart -> smartAlbumItems(ref.type, all, nowMillis)
    is AlbumRef.Folder -> all.filter { it.relativePath == ref.path }.sortedByDescending { it.dateMillis }
}

/**
 * Lista de álbumes: manuales (favoritos primero, como los entrega el repositorio), automáticos no vacíos y carpetas.
 * Los álbumes ocultos solo aparecen con [showHidden].
 */
fun albumOverview(all: List<MediaItem>, albums: List<Album>, nowMillis: Long, showHidden: Boolean): AlbumOverview {
    val byId = all.associateBy { it.id }
    val manual = albums.filter { showHidden || !it.isHidden }.map { a ->
        val items = a.mediaIds.mapNotNull { byId[it] }
        AlbumCard(AlbumRef.Manual(a.id), a.name, items.size, a.coverMediaId?.let { byId[it] } ?: items.firstOrNull(), a.isFavorite, a.isHidden)
    }
    val smart = SmartType.entries.mapNotNull { t ->
        val items = smartAlbumItems(t, all, nowMillis)
        if (items.isEmpty()) null else AlbumCard(AlbumRef.Smart(t), "", items.size, items.first())
    }
    val folders = folderAlbums(all).map { AlbumCard(AlbumRef.Folder(it.path), it.name, it.items.size, it.items.first()) }
    return AlbumOverview(manual, smart, folders)
}
