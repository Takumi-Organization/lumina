package com.adrian.lumina.domain.model

/** Cuánto de la biblioteca puede leer Lumina con los permisos actuales. */
enum class MediaAccess { NONE, PARTIAL, FULL }

/**
 * Nivel de acceso según los permisos concedidos. Puro (sin Android) para poder probarlo.
 * - API ≥ 33: acceso completo = fotos Y vídeos. Solo uno de ellos, o «fotos seleccionadas» (API 34+), es parcial.
 * - API < 33: un único permiso de almacenamiento.
 */
fun mediaAccess(sdk: Int, images: Boolean, video: Boolean, userSelected: Boolean, legacyRead: Boolean): MediaAccess = when {
    sdk >= 33 -> when {
        images && video -> MediaAccess.FULL
        images || video || (sdk >= 34 && userSelected) -> MediaAccess.PARTIAL
        else -> MediaAccess.NONE
    }
    else -> if (legacyRead) MediaAccess.FULL else MediaAccess.NONE
}

/** Resultado de la última lectura de MediaStore, para no mostrar una pantalla vacía muda. */
data class LibraryStatus(
    val loaded: Boolean = false,
    /** Mensaje técnico si la consulta falló (excepción); null si no hubo error. */
    val error: String? = null,
    /** Resumen legible de dónde se buscó y cuántas filas dio cada fuente. */
    val detail: String = "",
)

/** Tipo de medio según el MIME, para filas que MediaStore no clasificó como imagen/vídeo. */
fun kindFromMime(mime: String?): MediaKind? = when {
    mime == null -> null
    mime.startsWith("image/", ignoreCase = true) -> MediaKind.IMAGE
    mime.startsWith("video/", ignoreCase = true) -> MediaKind.VIDEO
    else -> null
}
