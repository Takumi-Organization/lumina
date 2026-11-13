package com.adrian.lumina

import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.MediaKind
import com.adrian.lumina.domain.usecase.MetadataSearchProvider
import com.adrian.lumina.domain.usecase.normalizeText
import com.adrian.lumina.domain.usecase.parseQuery
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class SearchTest {
    private val utc = ZoneOffset.UTC
    private fun ms(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).atStartOfDay(utc).toInstant().toEpochMilli()
    private fun m(id: Long, name: String, date: Long, path: String = "DCIM/Camera/", video: Boolean = false, fav: Boolean = false) =
        MediaItem(id, "u$id", name, if (video) MediaKind.VIDEO else MediaKind.IMAGE, date, 1, 1, 0, 1, "b", path, fav)
    private val items = listOf(
        m(1, "IMG_Playa_001.jpg", ms(2023, 8, 10)),
        m(2, "Playa_video.mp4", ms(2023, 8, 11), video = true),
        m(3, "Cumpleaños.jpg", ms(2022, 3, 5), path = "Pictures/Familia/", fav = true),
        m(4, "Screenshot_20240101.png", ms(2024, 1, 1), path = "Pictures/Screenshots/"),
        m(5, "foto.jpg", ms(2024, 1, 20), path = "Pictures/Playa/"),
    )
    private val provider = MetadataSearchProvider(utc)
    private suspend fun ids(q: String) = provider.search(q, items).map { it.item.id }

    @Test fun normalizeStripsAccentsAndCase() = assertEquals("cumpleanos video", normalizeText("Cumpleaños Vídeo"))

    @Test fun parseExtractsKeywords() {
        val q = parseQuery("Vídeos playa agosto 2023")
        assertEquals(MediaKind.VIDEO, q.kind); assertEquals(8, q.month); assertEquals(2023, q.year); assertEquals(listOf("playa"), q.terms)
    }

    @Test fun parseEnglishAndFlags() {
        val q = parseQuery("favorites screenshots january")
        assertTrue(q.favoritesOnly && q.screenshotsOnly); assertEquals(1, q.month); assertTrue(q.terms.isEmpty())
    }

    @Test fun numberThatIsNotAYearStaysAsTerm() = assertEquals(listOf("0012"), parseQuery("0012").terms)

    @Test fun blankQueryReturnsNothing() = runTest { assertEquals(emptyList<Long>(), ids("  ")) }

    @Test fun nameBeatsFolderAndPrefixBeatsContains() = runTest {
        // «playa»: id 2 (nombre empieza) > id 1 (nombre contiene) > id 5 (solo carpeta)
        assertEquals(listOf(2L, 1L, 5L), ids("playa"))
    }

    @Test fun accentInsensitiveBothWays() = runTest {
        assertEquals(listOf(3L), ids("cumpleanos"))
        assertEquals(listOf(3L), ids("CUMPLEAÑOS"))
    }

    @Test fun kindFilter() = runTest {
        assertEquals(listOf(2L), ids("videos"))
        assertEquals(setOf(1L, 3L, 4L, 5L), ids("fotos").toSet())
    }

    @Test fun yearAndMonthCombine() = runTest {
        assertEquals(setOf(1L, 2L), ids("2023").toSet())
        assertEquals(setOf(4L, 5L), ids("enero 2024").toSet())
        assertEquals(listOf(1L), ids("playa fotos agosto"))
    }

    @Test fun favoritesAndScreenshots() = runTest {
        assertEquals(listOf(3L), ids("favoritas"))
        assertEquals(listOf(4L), ids("capturas"))
    }

    @Test fun allTermsMustMatch() = runTest {
        assertEquals(emptyList<Long>(), ids("playa familia"))
        assertEquals(listOf(1L), ids("playa img"))
    }

    @Test fun tiesOrderedNewestFirst() = runTest { assertEquals(listOf(5L, 4L), ids("2024")) }
}
