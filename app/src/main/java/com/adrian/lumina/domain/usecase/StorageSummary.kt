package com.adrian.lumina.domain.usecase

import com.adrian.lumina.domain.model.MediaItem

data class StorageSummary(val photos: Int, val videos: Int, val photoBytes: Long, val videoBytes: Long) {
    val totalBytes get() = photoBytes + videoBytes
    val count get() = photos + videos
}

fun storageSummary(items: List<MediaItem>): StorageSummary {
    val (videos, photos) = items.partition { it.isVideo }
    return StorageSummary(photos.size, videos.size, photos.sumOf { it.sizeBytes }, videos.sumOf { it.sizeBytes })
}
