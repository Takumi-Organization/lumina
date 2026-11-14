package com.adrian.lumina

import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.MediaKind
import com.adrian.lumina.domain.usecase.layoutMosaic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MosaicTest {
    private fun m(id: Long, w: Int = 400, h: Int = 300) = MediaItem(id, "u$id", "$id.jpg", MediaKind.IMAGE, 1000 - id, w, h, 0, 1, "", "", false)
    private val many = (1L..40L).map { i -> m(i, if (i % 3L == 0L) 300 else 400, if (i % 3L == 0L) 400 else 300) }

    @Test fun everyItemAppearsExactlyOnceInOrder() {
        val rows = layoutMosaic(many, 3f)
        assertEquals(many.map { it.id }, rows.flatMap { r -> r.cells.map { it.item.id } })
    }

    @Test fun emptyHasNoRows() = assertTrue(layoutMosaic(emptyList(), 3f).isEmpty())

    @Test fun rowAspectNeverSmallerThanItsCellsAndLeftoverIsOnlyOnTheLastRow() {
        val rows = layoutMosaic(many, 3f)
        for ((i, r) in rows.withIndex()) {
            assertTrue(r.aspect + 1e-4f >= r.cells.sumOf { it.aspect.toDouble() }.toFloat())
            if (i < rows.lastIndex) assertEquals("fila $i", 0f, r.leftover, 1e-4f)
        }
    }

    @Test fun firstPhotoOfADayIsAHeroWhenDensityIsLow() {
        val rows = layoutMosaic(many.take(5), 3f)
        assertTrue(rows.first().hero); assertEquals(1, rows.first().cells.size)
        assertFalse(rows.drop(1).any { it.hero })
    }

    @Test fun noHeroWhenDenseOrFewPhotos() {
        assertFalse(layoutMosaic(many.take(5), 1.5f).any { it.hero })
        assertFalse(layoutMosaic(many.take(2), 3f).any { it.hero })
    }

    @Test fun rowLengthsVaryAcrossRows() {
        val sizes = layoutMosaic((1L..60L).map { m(it) }, 3f).filter { !it.hero }.dropLast(1).map { it.cells.size }.toSet()
        assertTrue("el ritmo debe variar: $sizes", sizes.size >= 2)
    }

    @Test fun extremeAspectsAreClamped() {
        val rows = layoutMosaic(listOf(m(1, 5000, 100), m(2, 100, 5000)), 3f)
        rows.flatMap { it.cells }.forEach { assertTrue(it.aspect in 0.62f..1.9f) }
    }

    @Test fun missingSizeCountsAsSquare() {
        val c = layoutMosaic(listOf(m(1, 0, 0)), 3f).single().cells.single()
        assertEquals(1f, c.aspect, 0f)
    }

    @Test fun deterministic() = assertEquals(layoutMosaic(many, 3f), layoutMosaic(many, 3f))
}
