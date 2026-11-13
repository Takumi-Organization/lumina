package com.adrian.lumina.domain.repository

import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.SearchHit

/**
 * Punto de extensión de la búsqueda. Hoy solo existe [com.adrian.lumina.domain.usecase.MetadataSearchProvider]
 * (nombre, carpeta, tipo, fecha, favoritos). Un futuro proveedor de reconocimiento de imagen se añadiría aquí
 * sin tocar la interfaz de usuario; no hay ninguno implementado ni simulado.
 */
interface SearchProvider {
    val id: String
    suspend fun search(query: String, items: List<MediaItem>): List<SearchHit>
}
