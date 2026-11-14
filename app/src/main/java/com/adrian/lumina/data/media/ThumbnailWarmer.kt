package com.adrian.lumina.data.media

import android.content.ContentResolver
import android.net.Uri
import android.os.Process
import android.util.Log
import android.util.Size
import com.adrian.lumina.domain.model.MediaItem
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/**
 * Calienta en segundo plano la caché de miniaturas del sistema: pide `loadThumbnail` de los elementos más recientes (a
 * baja prioridad y de dos en dos) para que, al desplazarse, las miniaturas ya existan y no haya que decodificar la foto
 * original. MediaProvider las guarda en disco, así que solo cuesta la primera vez. Se ejecuta una vez por proceso.
 */
class ThumbnailWarmer(private val resolver: ContentResolver, private val sizePx: Int = 320, private val limit: Int = 4000) {
    private val started = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(2))

    fun warm(items: List<MediaItem>) {
        if (items.isEmpty() || !started.compareAndSet(false, true)) return
        val targets = items.asSequence().take(limit).map { it.uri }.toList()
        scope.launch {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            val t0 = System.nanoTime()
            var done = 0
            for (uri in targets) {
                ensureActive()
                runCatching { resolver.loadThumbnail(Uri.parse(uri), Size(sizePx, sizePx), null).recycle(); done++ }
            }
            Log.i("LuminaPerf", "calentamiento de miniaturas: $done/${targets.size} en ${(System.nanoTime() - t0) / 1_000_000} ms")
        }
    }
}
