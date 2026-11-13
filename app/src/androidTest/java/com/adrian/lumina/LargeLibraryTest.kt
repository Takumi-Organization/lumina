package com.adrian.lumina

import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.launch
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.MediaKind
import com.adrian.lumina.domain.usecase.buildTimeline
import com.adrian.lumina.ui.gallery.GalleryActions
import com.adrian.lumina.ui.gallery.GalleryScreen
import com.adrian.lumina.ui.gallery.GalleryUiState
import com.adrian.lumina.ui.theme.LuminaTheme
import java.time.ZoneId
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Biblioteca de 12 000 elementos sintéticos (URIs falsas: las miniaturas fallan, así que mide estructura de lista,
 * no decodificación de imágenes). Imprime tiempos y memoria con el prefijo PERF; en el emulador, no en un móvil.
 */
@RunWith(AndroidJUnit4::class)
class LargeLibraryTest {
    @get:Rule val rule = createComposeRule()

    private val zone = ZoneId.systemDefault()
    private val n = 12_000
    private val items = List(n) { i ->
        MediaItem(i.toLong(), "content://media/none/$i", "IMG_$i.jpg", MediaKind.IMAGE,
            1_700_000_000_000L - i * 1_800_000L, 10, 10, 0, 1, "Camera", "DCIM/Camera/", false)
    }

    private fun usedMb() = Runtime.getRuntime().let { (it.totalMemory() - it.freeMemory()) / 1_048_576 }

    @Test fun timelineBuildsFastAndGridScrollsToTheEnd() {
        val t0 = System.nanoTime()
        val timeline = buildTimeline(items, zone)
        val buildMs = (System.nanoTime() - t0) / 1_000_000
        println("PERF emulador timeline($n): $buildMs ms")
        assertTrue("timeline tardó $buildMs ms", buildMs < 3_000)

        lateinit var state: androidx.compose.foundation.lazy.grid.LazyGridState
        lateinit var scope: kotlinx.coroutines.CoroutineScope
        rule.setContent {
            state = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
            scope = androidx.compose.runtime.rememberCoroutineScope()
            LuminaTheme { GalleryScreen(GalleryUiState.Content(timeline, n), emptySet(), 4, false, false, GalleryActions(), gridState = state) }
        }
        rule.waitForIdle()
        val before = usedMb()
        val last = timeline.entries.lastIndex
        val s0 = System.nanoTime()
        for (target in listOf(last, 0, last / 2)) {
            rule.runOnIdle { scope.launch { state.scrollToItem(target) } }
            rule.waitForIdle()
        }
        val scrollMs = (System.nanoTime() - s0) / 1_000_000
        println("PERF emulador 3 saltos de scroll en ${timeline.entries.size} filas: $scrollMs ms; heap antes=$before MB después=${usedMb()} MB")
        assertTrue("el scroll tardó $scrollMs ms", scrollMs < 20_000)
    }
}
