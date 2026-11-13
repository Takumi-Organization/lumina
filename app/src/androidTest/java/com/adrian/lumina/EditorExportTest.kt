package com.adrian.lumina

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adrian.lumina.data.media.ImageCodec
import com.adrian.lumina.editor.EditPipeline
import com.adrian.lumina.editor.EditState
import com.adrian.lumina.editor.PixelImage
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Guardar copia real en MediaStore (Pictures/Lumina): dimensiones, original intacto, copia distinta. */
@RunWith(AndroidJUnit4::class)
class EditorExportTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val codec = ImageCodec(context)
    private val created = mutableListOf<Uri>()

    @After fun cleanup() { created.forEach { runCatching { context.contentResolver.delete(it, null, null) } } }

    private fun gradient(w: Int, h: Int) = PixelImage(w, h, IntArray(w * h) { i ->
        val x = i % w; val y = i / w
        (0xFF shl 24) or ((x * 255 / w) shl 16) or ((y * 255 / h) shl 8) or 90
    })

    @Test fun savesRotatedCopyWithoutTouchingOriginal() {
        val original = codec.saveCopy(gradient(400, 300), "prueba.jpg").also { created += it }
        val before = codec.decode(original.toString(), 4096)
        assertEquals(400, before.width); assertEquals(300, before.height)

        val edited = EditPipeline.apply(before, EditState(quarterTurns = 1, exposure = 0.3f))
        val copy = codec.saveCopy(edited, "prueba.jpg").also { created += it }
        assertNotEquals(original, copy)

        val after = codec.decode(copy.toString(), 4096)
        assertEquals(300, after.width); assertEquals(400, after.height)
        assertEquals("el original no debe cambiar", before, codec.decode(original.toString(), 4096))

        val path = context.contentResolver.query(copy, arrayOf(android.provider.MediaStore.Images.Media.RELATIVE_PATH), null, null, null)
            .use { it!!.moveToFirst(); it.getString(0) }
        assertTrue(path, path.startsWith("Pictures/Lumina"))
    }

    @Test fun decodeDownscalesToMaxSide() {
        val uri = codec.saveCopy(gradient(800, 400), "grande.jpg").also { created += it }
        val small = codec.decode(uri.toString(), 200)
        assertEquals(200, small.width); assertEquals(100, small.height)
    }

    @Test fun editedCopyKeepsExifFromTheOriginal() {
        val original = codec.saveCopy(gradient(400, 300), "exif.jpg").also { created += it }
        context.contentResolver.openFileDescriptor(original, "rw")!!.use { pfd ->
            val e = androidx.exifinterface.media.ExifInterface(pfd.fileDescriptor)
            e.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_MAKE, "Lumina Labs")
            e.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_MODEL, "Prueba 9")
            e.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL, "2021:03:04 05:06:07")
            e.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, "6")
            e.saveAttributes()
        }
        val edited = EditPipeline.apply(codec.decode(original.toString(), 4096), EditState(quarterTurns = 1))
        val copy = codec.saveCopy(edited, "exif.jpg", original.toString()).also { created += it }
        val e = context.contentResolver.openInputStream(copy)!!.use { androidx.exifinterface.media.ExifInterface(it) }
        assertEquals("Lumina Labs", e.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MAKE))
        assertEquals("Prueba 9", e.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MODEL))
        assertEquals("2021:03:04 05:06:07", e.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL))
        assertEquals(1, e.getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, -1))
    }
}
