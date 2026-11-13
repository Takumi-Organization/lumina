package com.adrian.lumina.domain.model

/** Álbum manual del usuario: guarda referencias (ids de MediaStore) en orden, no copia archivos. */
data class Album(
    val id: Long,
    val name: String,
    val description: String,
    val coverMediaId: Long?,
    val isFavorite: Boolean,
    val isHidden: Boolean,
    val mediaIds: List<Long>,
)

/** Categorías automáticas derivadas de metadatos de MediaStore. */
enum class SmartType { RECENT, CAMERA, SCREENSHOTS, DOWNLOADS, PHOTOS, VIDEOS, FAVORITES }

/** Identifica qué se está viendo: álbum manual, categoría automática o carpeta del dispositivo. */
sealed interface AlbumRef {
    data class Manual(val id: Long) : AlbumRef
    data class Smart(val type: SmartType) : AlbumRef
    data class Folder(val path: String) : AlbumRef
}

enum class AlbumSort { MANUAL, DATE_DESC, DATE_ASC, NAME }
