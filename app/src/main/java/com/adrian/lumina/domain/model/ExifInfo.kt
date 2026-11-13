package com.adrian.lumina.domain.model

/** Metadatos EXIF relevantes para el usuario; todo opcional porque muchas imágenes no los traen. */
data class ExifInfo(
    val make: String? = null,
    val model: String? = null,
    val aperture: Double? = null,
    val exposureSeconds: Double? = null,
    val iso: Int? = null,
    val focalLengthMm: Double? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    val hasLocation get() = latitude != null && longitude != null
    val camera get() = listOfNotNull(make, model).joinToString(" ").ifBlank { null }
}
