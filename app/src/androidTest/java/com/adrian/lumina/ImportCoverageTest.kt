package com.adrian.lumina

import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adrian.lumina.data.media.MediaStoreSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ¿Aparece en Lumina todo lo que MediaStore indexa? Inserta archivos de tipos variados por las distintas colecciones
 * (Imágenes, Vídeo, Descargas, Archivos) y comprueba que la biblioteca los lista.
 */
@RunWith(AndroidJUnit4::class)
class ImportCoverageTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val resolver = context.contentResolver
    private val created = mutableListOf<Uri>()
    private val tag = "LuminaCov${System.nanoTime()}"

    @After fun cleanup() { created.forEach { runCatching { resolver.delete(it, null, null) } } }

    private val gif = byteArrayOf(0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 1, 0, 1, 0, 0, 0, 0, 0x3B)
    private val junk = ByteArray(64) { it.toByte() }

    private fun insert(collection: Uri, name: String, mime: String, dir: String, bytes: ByteArray): Uri {
        val v = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$tag-$name")
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$dir/$tag")
        }
        val uri = resolver.insert(collection, v) ?: error("no se pudo insertar $name")
        resolver.openOutputStream(uri)!!.use { it.write(bytes) }
        created += uri
        return uri
    }

    private fun library() = runBlocking { MediaStoreSource(resolver).observeAll().first() }.filter { it.name.startsWith(tag) }

    @Test fun everyIndexedImageAndVideoTypeIsListed() {
        val images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val videos = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        insert(images, "a.png", "image/png", "Pictures", junk)
        insert(images, "b.webp", "image/webp", "Pictures", junk)
        insert(images, "c.gif", "image/gif", "Pictures", gif)
        insert(images, "d.heic", "image/heic", "Pictures", junk)
        insert(images, "e.dng", "image/x-adobe-dng", "Pictures", junk)
        insert(images, "f.bmp", "image/bmp", "Pictures", junk)
        insert(videos, "g.mp4", "video/mp4", "Movies", junk)
        insert(videos, "h.webm", "video/webm", "Movies", junk)
        insert(videos, "i.mkv", "video/x-matroska", "Movies", junk)
        insert(videos, "j.3gp", "video/3gpp", "Movies", junk)
        // Descargas y colección genérica de archivos: el tipo de medio lo decide MediaStore por MIME.
        insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, "k.png", "image/png", "Download", junk)
        insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, "l.mp4", "video/mp4", "Download", junk)
        insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), "m.jpg", "image/jpeg", "Download", junk)

        val names = library().map { it.name.removePrefix("$tag-") }.toSet()
        val expected = setOf("a.png", "b.webp", "c.gif", "d.heic", "e.dng", "f.bmp", "g.mp4", "h.webm", "i.mkv", "j.3gp", "k.png", "l.mp4", "m.jpg")
        assertEquals("faltan: ${expected - names}", expected, names)
    }

    @Test fun pendingFilesAreNotListedUntilFinished() {
        val v = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$tag-pend.jpg"); put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/$tag"); put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v)!!.also { created += it }
        assertTrue(library().none { it.name.endsWith("pend.jpg") })
        resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        assertTrue(library().any { it.name.endsWith("pend.jpg") })
    }
}
