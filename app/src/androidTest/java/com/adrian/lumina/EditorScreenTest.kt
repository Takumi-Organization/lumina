package com.adrian.lumina

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adrian.lumina.data.media.ImageCodec
import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.MediaKind
import com.adrian.lumina.editor.PixelImage
import com.adrian.lumina.ui.editor.EditorScreen
import com.adrian.lumina.ui.editor.EditorViewModel
import com.adrian.lumina.ui.theme.LuminaTheme
import java.io.File
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** El editor completo con una imagen real de MediaStore: recorte libre arrastrando, deshacer y guardado. */
@RunWith(AndroidJUnit4::class)
class EditorScreenTest {
    @get:Rule val rule = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val codec = ImageCodec(context)
    private val created = mutableListOf<android.net.Uri>()

    @After fun cleanup() { created.forEach { runCatching { context.contentResolver.delete(it, null, null) } } }

    private fun str(id: Int) = context.getString(id)

    @Test fun freeCropByDraggingCornerThenUndo() {
        val w = 400; val h = 300
        val img = PixelImage(w, h, IntArray(w * h) { i -> (0xFF shl 24) or ((i % w) * 255 / w shl 16) or ((i / w) * 255 / h shl 8) or 80 })
        val uri = codec.saveCopy(img, "recorte.jpg").also { created += it }
        val item = MediaItem(1, uri.toString(), "recorte.jpg", MediaKind.IMAGE, 0, w, h, 0, 1, "Lumina", "Pictures/Lumina/", false)
        val vm = EditorViewModel(item, codec)
        rule.setContent { LuminaTheme(dark = true) { EditorScreen(vm, onClose = {}) } }
        rule.waitUntil(10_000) { vm.preview.value != null }

        rule.onNodeWithText(str(R.string.editor_tab_crop)).performClick()
        rule.waitUntil(10_000) { vm.cropMode.value }
        rule.waitForIdle()
        val overlay = rule.onNodeWithTag("crop-overlay")
        overlay.performTouchInput {
            val fitH = width / (w.toFloat() / h)
            val top = (height - fitH) / 2f
            // Esquina superior izquierda de la imagen → hacia dentro (30 % del ancho y del alto).
            swipe(Offset(4f, top + 4f), Offset(width * 0.3f, top + fitH * 0.3f), 400)
        }
        rule.waitForIdle()
        val c = vm.live.value.crop
        assertTrue("crop.left=${c.left}", c.left in 0.2f..0.4f)
        assertTrue("crop.top=${c.top}", c.top in 0.2f..0.4f)
        assertTrue(c.right == 1f && c.bottom == 1f)
        assertTrue(vm.canUndo.value)

        // Captura para revisión visual.
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        File(context.getExternalFilesDir(null), "editor-crop.png").outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }

        rule.onNodeWithTag("editor-undo").performClick()
        rule.waitForIdle()
        assertTrue(vm.live.value.crop.isFull)
    }
}
