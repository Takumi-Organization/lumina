package com.adrian.lumina.app

import android.content.Context
import com.adrian.lumina.data.media.MediaStoreSource
import com.adrian.lumina.domain.repository.MediaRepository

/** Inyección manual: pocas dependencias, sin framework (ver docs/ARQUITECTURA.md). */
class AppContainer(context: Context) {
    val media: MediaRepository = MediaStoreSource(context.contentResolver)
}
