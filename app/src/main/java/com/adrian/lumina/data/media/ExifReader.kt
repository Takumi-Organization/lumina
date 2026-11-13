package com.adrian.lumina.data.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.adrian.lumina.domain.model.ExifInfo
import com.adrian.lumina.domain.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lee EXIF del archivo original. Sin ACCESS_MEDIA_LOCATION Android elimina el GPS del flujo,
 * así que la ubicación solo aparece si el usuario concedió ese permiso.
 */
class ExifReader(private val context: Context) {

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED

    suspend fun read(item: MediaItem): ExifInfo? = withContext(Dispatchers.IO) {
        if (item.isVideo) return@withContext null
        val uri = Uri.parse(item.uri)
        val source = if (hasLocationPermission()) {
            try { MediaStore.setRequireOriginal(uri) } catch (_: UnsupportedOperationException) { uri }
        } else uri
        try {
            context.contentResolver.openInputStream(source)?.use { stream ->
                val e = ExifInterface(stream)
                val ll = e.latLong
                ExifInfo(
                    make = e.getAttribute(ExifInterface.TAG_MAKE)?.trim()?.ifBlank { null },
                    model = e.getAttribute(ExifInterface.TAG_MODEL)?.trim()?.ifBlank { null },
                    aperture = e.getAttributeDouble(ExifInterface.TAG_F_NUMBER, -1.0).takeIf { it > 0 },
                    exposureSeconds = e.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, -1.0).takeIf { it > 0 },
                    iso = e.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, -1).takeIf { it > 0 },
                    focalLengthMm = e.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, -1.0).takeIf { it > 0 },
                    latitude = ll?.get(0), longitude = ll?.get(1),
                )
            }
        } catch (_: Exception) { null } // archivo borrado o ilegible: se muestra sin EXIF
    }
}
