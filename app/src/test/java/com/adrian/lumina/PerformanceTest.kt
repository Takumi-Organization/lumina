package com.adrian.lumina

import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.MediaKind
import com.adrian.lumina.domain.usecase.*
import com.adrian.lumina.editor.EditPipeline
import com.adrian.lumina.editor.EditState
import com.adrian.lumina.editor.PixelImage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

/**
 * Medición de dominio en JVM (no sustituye a medir frames en un dispositivo). Los límites son holgados:
 * detectan regresiones grandes (p. ej. algoritmos cuadráticos), no afinan rendimiento. Los tiempos se imprimen.
 */
class PerformanceTest {
    private val n = 50_000
    private val now = 1_700_000_000_000L
    private val items = List(n) { i ->
        val path = when (i % 5) { 0 -> "DCIM/Camera/"; 1 -> "Pictures/Screenshots/"; 2 -> "Download/"; 3 -> "Pictures/Memes/"; else -> "Movies/" }
        MediaItem(i.toLong(), "content://media/$i", "IMG_$i.jpg", if (i % 7 == 0) MediaKind.VIDEO else MediaKind.IMAGE,
            now - i * 3_600_000L, 4000, 3000, 0, 3_000_000, "b", path, i % 50 == 0)
    }

    private inline fun <T> timed(label: String, limitMs: Long, block: () -> T): T {
        block() // calentamiento de JIT
        val t0 = System.nanoTime()
        val r = block()
        val ms = (System.nanoTime() - t0) / 1_000_000
        println("PERF $label: $ms ms (n=$n)")
        assertTrue("$label tardó $ms ms (límite $limitMs)", ms <= limitMs)
        return r
    }

    @Test fun timelineOf50kItems() {
        val t = timed("buildTimeline", 3_000) { buildTimeline(items, ZoneOffset.UTC) }
        assertEquals(n, t.entries.count { it is com.adrian.lumina.domain.model.TimelineEntry.Cell })
    }

    @Test fun overviewOf50kItems() {
        val o = timed("albumOverview", 3_000) { albumOverview(items, emptyList(), now, false) }
        assertTrue(o.folders.isNotEmpty())
    }

    @Test fun searchOver50kItems() = runTest {
        val provider = MetadataSearchProvider(ZoneOffset.UTC)
        var hits = 0
        val t0 = System.nanoTime(); provider.search("img 49 fotos", items)
        val t1 = System.nanoTime()
        hits = provider.search("img 49 fotos", items).size
        val ms = (System.nanoTime() - t1) / 1_000_000
        println("PERF search: $ms ms (n=$n, hits=$hits, primera ${(t1 - t0) / 1_000_000} ms)")
        assertTrue("búsqueda tardó $ms ms", ms <= 3_000)
        assertTrue(hits > 0)
    }

    @Test fun exportPipelineOn12Megapixels() {
        val w = 4000; val h = 3000
        val img = PixelImage(w, h, IntArray(w * h) { (0xFF shl 24) or (it * 2654435761L).toInt().and(0xFFFFFF) })
        val st = EditState(exposure = 0.2f, contrast = 0.2f, saturation = 0.1f, warmth = 0.1f, shadows = 0.3f, highlights = -0.2f, sharpen = 0.5f, vignette = 0.3f, quarterTurns = 1)
        val t0 = System.nanoTime()
        val out = EditPipeline.apply(img, st)
        val ms = (System.nanoTime() - t0) / 1_000_000
        println("PERF editor 12MP completo: $ms ms (JVM, 1 hilo)")
        assertEquals(3000, out.width); assertEquals(4000, out.height)
        assertTrue("el pipeline completo tardó $ms ms", ms <= 30_000)
    }
}
