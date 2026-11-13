package com.adrian.lumina.data.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Environment
import androidx.exifinterface.media.ExifInterface
import android.provider.MediaStore
import com.adrian.lumina.editor.PixelImage
import kotlin.math.max
import kotlin.math.roundToInt

/** Puente entre `Bitmap`/MediaStore y [PixelImage]. Aquí vive todo lo de Android del editor. */
class ImageCodec(private val context: Context) {

    /** Decodifica con la orientación EXIF ya aplicada, reducida para que el lado mayor no pase de [maxSide]. */
    fun decode(uri: String, maxSide: Int): PixelImage {
        val source = ImageDecoder.createSource(context.contentResolver, Uri.parse(uri))
        val bmp = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = max(info.size.width, info.size.height)
            if (longest > maxSide) {
                val f = maxSide.toFloat() / longest
                decoder.setTargetSize((info.size.width * f).roundToInt().coerceAtLeast(1), (info.size.height * f).roundToInt().coerceAtLeast(1))
            }
        }
        return toPixels(bmp).also { bmp.recycle() }
    }

    /** Guarda una copia nueva en Pictures/Lumina (nunca sobrescribe el original). Devuelve su Uri de MediaStore. */
    fun saveCopy(image: PixelImage, originalName: String, sourceUri: String? = null, quality: Int = 95): Uri {
        val png = originalName.endsWith(".png", ignoreCase = true)
        val base = originalName.substringBeforeLast('.', originalName)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "${base}_lumina_${System.currentTimeMillis()}.${if (png) "png" else "jpg"}")
            put(MediaStore.Images.Media.MIME_TYPE, if (png) "image/png" else "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Lumina")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("MediaStore no creó el archivo")
        try {
            val bmp = toBitmap(image)
            resolver.openOutputStream(uri)!!.use { out ->
                check(bmp.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, quality, out)) { "No se pudo codificar la imagen" }
            }
            bmp.recycle()
            if (!png && sourceUri != null) copyExif(sourceUri, uri)
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        } catch (t: Throwable) {
            resolver.delete(uri, null, null) // no dejar un archivo a medias
            throw t
        }
        return uri
    }

    /**
     * Copia a la copia editada los metadatos EXIF del original (fecha, cámara, exposición, GPS). La orientación se
     * restablece porque los píxeles ya están girados. Si falla (p. ej. sin permiso de ubicación) la copia se conserva sin ellos.
     */
    private fun copyExif(sourceUri: String, dest: Uri) {
        runCatching {
            var src = Uri.parse(sourceUri)
            if (context.checkSelfPermission(android.Manifest.permission.ACCESS_MEDIA_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                src = MediaStore.setRequireOriginal(src)
            }
            val from = context.contentResolver.openInputStream(src)!!.use { ExifInterface(it) }
            context.contentResolver.openFileDescriptor(dest, "rw")!!.use { pfd ->
                val to = ExifInterface(pfd.fileDescriptor)
                for (tag in EXIF_TAGS) from.getAttribute(tag)?.let { to.setAttribute(tag, it) }
                to.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                to.saveAttributes()
            }
        }.onFailure { android.util.Log.w("Lumina", "No se pudieron copiar los metadatos EXIF", it) }
    }

    companion object {
        private val EXIF_TAGS = listOf(
            ExifInterface.TAG_DATETIME, ExifInterface.TAG_DATETIME_ORIGINAL, ExifInterface.TAG_DATETIME_DIGITIZED,
            ExifInterface.TAG_OFFSET_TIME, ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
            ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL, ExifInterface.TAG_LENS_MODEL, ExifInterface.TAG_SOFTWARE,
            ExifInterface.TAG_F_NUMBER, ExifInterface.TAG_EXPOSURE_TIME, ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
            ExifInterface.TAG_FOCAL_LENGTH, ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, ExifInterface.TAG_FLASH,
            ExifInterface.TAG_WHITE_BALANCE, ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
            ExifInterface.TAG_GPS_LATITUDE, ExifInterface.TAG_GPS_LATITUDE_REF, ExifInterface.TAG_GPS_LONGITUDE,
            ExifInterface.TAG_GPS_LONGITUDE_REF, ExifInterface.TAG_GPS_ALTITUDE, ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_TIMESTAMP, ExifInterface.TAG_GPS_DATESTAMP,
        )

        fun toBitmap(image: PixelImage): Bitmap =
            Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888).also {
                it.setPixels(image.pixels, 0, image.width, 0, 0, image.width, image.height)
            }

        fun toPixels(bmp: Bitmap): PixelImage {
            val px = IntArray(bmp.width * bmp.height)
            bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
            return PixelImage(bmp.width, bmp.height, px)
        }
    }
}
