package com.adrian.lumina.domain.model

/** Consulta ya interpretada. Todas las condiciones presentes deben cumplirse (AND). */
data class SearchQuery(
    /** Términos libres, normalizados (sin acentos, minúsculas): deben aparecer en el nombre o la carpeta. */
    val terms: List<String> = emptyList(),
    val kind: MediaKind? = null,
    val year: Int? = null,
    /** 1..12 */
    val month: Int? = null,
    val favoritesOnly: Boolean = false,
    val screenshotsOnly: Boolean = false,
) {
    val isEmpty get() = terms.isEmpty() && kind == null && year == null && month == null && !favoritesOnly && !screenshotsOnly
}

data class SearchHit(val item: MediaItem, val score: Int)
