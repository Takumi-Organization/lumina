package com.adrian.lumina.domain.model

enum class MediaKind { IMAGE, VIDEO }

/** Metadatos de un elemento de MediaStore. Sin dependencias de Android. */
data class MediaItem(
    val id: Long,
    val uri: String,
    val name: String,
    val kind: MediaKind,
    /** Epoch ms de captura (o de modificación si no hay). */
    val dateMillis: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val sizeBytes: Long,
    val bucketName: String,
    val relativePath: String,
    val isFavorite: Boolean,
) {
    val isVideo get() = kind == MediaKind.VIDEO
}
