package com.adrian.lumina.domain.usecase

import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.MediaKind
import com.adrian.lumina.domain.model.SearchHit
import com.adrian.lumina.domain.model.SearchQuery
import com.adrian.lumina.domain.repository.SearchProvider
import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId

/** Minúsculas y sin diacríticos: «Vídeos» → «videos». */
fun normalizeText(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()

private val MONTHS: Map<String, Int> = buildMap {
    listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
        .forEachIndexed { i, n -> put(n, i + 1) }
    listOf("january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december")
        .forEachIndexed { i, n -> put(n, i + 1) }
    put("setiembre", 9)
}
private val VIDEO_WORDS = setOf("video", "videos", "clip", "clips", "pelicula", "movie", "movies")
private val PHOTO_WORDS = setOf("foto", "fotos", "imagen", "imagenes", "photo", "photos", "image", "images", "picture", "pictures")
private val FAVORITE_WORDS = setOf("favorita", "favoritas", "favorito", "favoritos", "favorite", "favorites")
private val SCREENSHOT_WORDS = setOf("captura", "capturas", "screenshot", "screenshots")

/** Interpreta palabras clave (tipo, mes, año, favoritas, capturas); el resto son términos libres. */
fun parseQuery(raw: String): SearchQuery {
    var q = SearchQuery()
    val terms = mutableListOf<String>()
    for (w in normalizeText(raw).split(Regex("[\\s,;]+")).filter { it.isNotBlank() }) {
        when {
            w in VIDEO_WORDS -> q = q.copy(kind = MediaKind.VIDEO)
            w in PHOTO_WORDS -> q = q.copy(kind = MediaKind.IMAGE)
            w in FAVORITE_WORDS -> q = q.copy(favoritesOnly = true)
            w in SCREENSHOT_WORDS -> q = q.copy(screenshotsOnly = true)
            w in MONTHS -> q = q.copy(month = MONTHS.getValue(w))
            w.length == 4 && w.all(Char::isDigit) && w.toInt() in 1970..2100 -> q = q.copy(year = w.toInt())
            else -> terms += w
        }
    }
    return q.copy(terms = terms)
}

/** Búsqueda local por metadatos (sin red, sin reconocimiento de imagen). */
class MetadataSearchProvider(private val zone: ZoneId = ZoneId.systemDefault()) : SearchProvider {
    override val id = "metadata"

    override suspend fun search(query: String, items: List<MediaItem>): List<SearchHit> {
        val q = parseQuery(query)
        if (q.isEmpty) return emptyList()
        return items.mapNotNull { item -> score(item, q)?.let { SearchHit(item, it) } }
            .sortedWith(compareByDescending<SearchHit> { it.score }.thenByDescending { it.item.dateMillis })
    }

    /** null = no cumple; si no, puntuación (nombre pesa más que carpeta). */
    internal fun score(item: MediaItem, q: SearchQuery): Int? {
        if (q.kind != null && item.kind != q.kind) return null
        if (q.favoritesOnly && !item.isFavorite) return null
        if (q.screenshotsOnly && !item.isScreenshot()) return null
        if (q.year != null || q.month != null) {
            val d = Instant.ofEpochMilli(item.dateMillis).atZone(zone)
            if (q.year != null && d.year != q.year) return null
            if (q.month != null && d.monthValue != q.month) return null
        }
        var score = 1
        val name = normalizeText(item.name)
        val folder = normalizeText(item.relativePath + " " + item.bucketName)
        for (t in q.terms) {
            score += when {
                name.startsWith(t) -> 4
                name.contains(t) -> 3
                folder.contains(t) -> 1
                else -> return null
            }
        }
        return score
    }
}
