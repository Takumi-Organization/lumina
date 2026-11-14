package com.adrian.lumina.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.crossfade
import coil3.SingletonImageLoader
import com.adrian.lumina.data.media.ThumbFetcher
import com.adrian.lumina.data.media.ThumbKeyer

class LuminaApp : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(ThumbKeyer())
                add(ThumbFetcher.Factory(contentResolver))
            }
            // Memoria acotada (antes crecía hasta ~1 GB nativo al recorrer toda la biblioteca) y aparición suave.
            .memoryCache { coil3.memory.MemoryCache.Builder().maxSizeBytes(120 * 1024 * 1024).build() }
            .crossfade(140)
            .build()
}
