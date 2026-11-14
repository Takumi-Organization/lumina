package com.adrian.lumina

import android.database.MatrixCursor
import android.provider.MediaStore
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adrian.lumina.data.media.MediaStoreSource
import com.adrian.lumina.domain.model.MediaKind
import com.adrian.lumina.ui.gallery.GalleryActions
import com.adrian.lumina.ui.gallery.GalleryScreen
import com.adrian.lumina.ui.gallery.GalleryUiState
import com.adrian.lumina.ui.theme.LuminaTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Un fallo de MediaStore nunca debe acabar en una pantalla vacía muda: queda en `status` y se ve en la UI. */
@RunWith(AndroidJUnit4::class)
class LibraryDiagnosticsTest {
    @get:Rule val rule = createComposeRule()
    private val resolver = ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
    private fun str(id: Int) = ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    private fun cursor(vararg rows: List<Any?>) = MatrixCursor(
        arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.MIME_TYPE, MediaStore.MediaColumns.DATE_ADDED, MediaStore.MediaColumns.SIZE)
    ).also { c -> rows.forEach { c.addRow(it) } }

    @Test fun securityExceptionBecomesVisibleStatusNotACrash() {
        val src = MediaStoreSource(resolver) { _, _, _, _ -> throw SecurityException("sin permiso de prueba") }
        val items = runBlocking { src.observeAll().first() }
        assertTrue(items.isEmpty())
        assertTrue(src.status.value.error!!.contains("SecurityException"))
        assertTrue(src.status.value.loaded)
    }

    @Test fun rejectedColumnFallsBackToMinimalProjection() {
        var calls = 0
        val src = MediaStoreSource(resolver) { _, proj, _, _ ->
            calls++
            if (proj.size > 8) throw IllegalArgumentException("columna desconocida") else cursor(listOf(1L, "a.jpg", "image/jpeg", 1000L, 5L))
        }
        val items = runBlocking { src.observeAll().first() }
        assertTrue(calls >= 2)
        assertTrue(items.any { it.name == "a.jpg" })
        assertNull(src.status.value.error)
    }

    @Test fun nullCursorIsEmptyButExplained() {
        val src = MediaStoreSource(resolver) { _, _, _, _ -> null }
        assertTrue(runBlocking { src.observeAll().first() }.isEmpty())
        assertNull(src.status.value.error)
        assertTrue("detalle: ${src.status.value.detail}", src.status.value.detail.contains("imágenes=0"))
    }

    @Test fun fallsBackToFilesCollectionAndClassifiesByMime() {
        val src = MediaStoreSource(resolver) { uri, _, _, _ ->
            // Imágenes/Vídeo vacías; solo la colección de Archivos devuelve filas (una sin clasificar con MIME de imagen).
            if (uri.toString().contains("/file")) cursor(listOf(7L, "raro.heic", "image/heic", 10L, 1L), listOf(8L, "x.mp4", "video/mp4", 20L, 1L), listOf(9L, "a.mp3", "audio/mpeg", 30L, 1L))
            else cursor()
        }
        val items = runBlocking { src.observeAll().first() }
        assertEquals(setOf("raro.heic" to MediaKind.IMAGE, "x.mp4" to MediaKind.VIDEO), items.map { it.name to it.kind }.toSet())
    }

    @Test fun refreshReadsAgainAfterPermissionIsGranted() {
        var granted = false
        val src = MediaStoreSource(resolver) { _, _, _, _ -> if (granted) cursor(listOf(1L, "b.jpg", "image/jpeg", 1L, 1L)) else throw SecurityException("denegado") }
        val seen = mutableListOf<Int>()
        runBlocking {
            val job = launch(Dispatchers.Default) { src.observeAll().collect { seen += it.size } }
            withTimeout(5_000) { while (seen.isEmpty()) delay(20) }
            granted = true
            src.refresh()
            withTimeout(5_000) { while (seen.lastOrNull() != 1 && seen.none { it >= 1 }) { src.refresh(); delay(50) } }
            job.cancel()
        }
        assertTrue(seen.first() == 0 && seen.any { it >= 1 })
        assertNull(src.status.value.error)
    }

    @Test fun failedStateShowsReasonAndRetry() {
        var retried = 0
        rule.setContent {
            LuminaTheme { GalleryScreen(GalleryUiState.Failed("SecurityException: x", "external/imágenes=error"), emptySet(), 4, false, false, GalleryActions(onRetry = { retried++ })) }
        }
        rule.onNodeWithText(str(R.string.library_failed_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.library_retry)).performClick()
        assertEquals(1, retried)
    }

    @Test fun emptyLibraryExplainsWhereItLookedAndOffersPermissions() {
        var perms = 0
        rule.setContent {
            LuminaTheme { GalleryScreen(GalleryUiState.Empty(false, "external/imágenes=0; external/vídeos=0"), emptySet(), 4, false, false, GalleryActions(onOpenPermissions = { perms++ })) }
        }
        rule.onNodeWithText("imágenes=0", substring = true).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.library_permissions)).performClick()
        assertEquals(1, perms)
    }
}
