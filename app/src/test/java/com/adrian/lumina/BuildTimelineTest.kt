package com.adrian.lumina

import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.MediaKind
import com.adrian.lumina.domain.model.TimelineEntry
import com.adrian.lumina.domain.usecase.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildTimelineTest {
    private val utc = ZoneId.of("UTC")

    private fun item(id: Long, date: String, hour: Int = 12) = MediaItem(
        id, "content://x/$id", "$id.jpg", MediaKind.IMAGE,
        LocalDate.parse(date).atTime(hour, 0).atZone(utc).toInstant().toEpochMilli(),
        100, 100, 0, 1, "Camera", "DCIM/Camera/", false,
    )

    @Test fun empty() = assertTrue(buildTimeline(emptyList(), utc).entries.isEmpty())

    @Test fun groupsByDayNewestFirstWithHeaders() {
        val t = buildTimeline(listOf(item(1, "2024-03-01"), item(2, "2024-03-05", 9), item(3, "2024-03-05", 18)), utc)
        val kinds = t.entries.map { if (it is TimelineEntry.Header) "H${it.count}" else "C${(it as TimelineEntry.Cell).item.id}" }
        assertEquals(listOf("H2", "C3", "C2", "H1", "C1"), kinds)
    }

    @Test fun monthIndexPointsToFirstEntryOfEachMonthWithCounts() {
        val t = buildTimeline(
            listOf(item(1, "2023-12-31"), item(2, "2024-01-02"), item(3, "2024-01-02", 20), item(4, "2024-03-09")), utc,
        )
        assertEquals(listOf(YearMonth.of(2024, 3), YearMonth.of(2024, 1), YearMonth.of(2023, 12)), t.months.map { it.month })
        assertEquals(listOf(1, 2, 1), t.months.map { it.count })
        assertEquals(listOf(0, 2, 5), t.months.map { it.entryIndex })
        assertTrue(t.entries[t.months[1].entryIndex] is TimelineEntry.Header)
    }

    @Test fun zoneChangesDayBoundary() {
        val late = item(1, "2024-03-01", 23) // 23:00 UTC
        val madrid = ZoneId.of("Asia/Tokyo") // +9 → día siguiente
        val h = buildTimeline(listOf(late), madrid).entries.first() as TimelineEntry.Header
        assertEquals(LocalDate.parse("2024-03-02"), h.day)
    }
}

class ScrubTest {
    private val utc = ZoneId.of("UTC")
    private fun it(id: Long, date: String) = MediaItem(
        id, "u$id", "$id.jpg", MediaKind.IMAGE,
        LocalDate.parse(date).atTime(12, 0).atZone(utc).toInstant().toEpochMilli(),
        1, 1, 0, 1, "", "", false,
    )
    // entradas: H(2024-03)=0 C=1 | H(2024-01)=2 C=3 C=4 | H(2022-05)=5 C=6
    private val t = buildTimeline(listOf(it(1, "2024-03-09"), it(2, "2024-01-02"), it(3, "2024-01-02"), it(4, "2022-05-05")), utc)

    @Test fun monthAtEntry() {
        assertEquals(YearMonth.of(2024, 3), t.monthAtEntry(1)?.month)
        assertEquals(YearMonth.of(2024, 1), t.monthAtEntry(2)?.month)
        assertEquals(YearMonth.of(2024, 1), t.monthAtEntry(4)?.month)
        assertEquals(YearMonth.of(2022, 5), t.monthAtEntry(6)?.month)
    }

    @Test fun fractionsAreInverse() {
        assertEquals(0f, t.fractionOfEntry(0), 0f)
        assertEquals(1f, t.fractionOfEntry(6), 0f)
        for (i in 0..6) assertEquals(i, t.entryAtFraction(t.fractionOfEntry(i)))
        assertEquals(6, t.entryAtFraction(5f)); assertEquals(0, t.entryAtFraction(-1f))
    }

    @Test fun yearTicksOnePerYear() {
        assertEquals(listOf(2024, 2022), t.yearTicks().map { it.year })
        assertEquals(0f, t.yearTicks()[0].fraction, 0f)
    }

    @Test fun toggleGroup() {
        assertEquals(setOf(1L, 2L, 3L), setOf(1L).toggleGroup(listOf(2, 3)))
        assertEquals(setOf(1L), setOf(1L, 2L, 3L).toggleGroup(listOf(2, 3)))
        assertEquals(setOf(1L), setOf(1L).toggleGroup(emptyList()))
    }
}

class ExifFormatTest {
    @Test fun exposure() {
        assertEquals("1/250 s", formatExposure(0.004))
        assertEquals("1/2 s", formatExposure(0.5))
        assertEquals("2 s", formatExposure(2.0))
        assertEquals("1.3 s", formatExposure(1.3))
    }
    @Test fun aperture() { assertEquals("f/1.8", formatAperture(1.8)); assertEquals("f/2", formatAperture(2.0)) }
    @Test fun focal() { assertEquals("26 mm", formatFocalLength(26.0)); assertEquals("5.4 mm", formatFocalLength(5.4)) }
    @Test fun coords() {
        assertEquals("40.4168° N, 3.7038° W", formatCoords(40.4168, -3.7038))
        assertEquals("33.8688° S, 151.2093° E", formatCoords(-33.8688, 151.2093))
    }
    @Test fun bytes() {
        val en = java.util.Locale.US
        assertEquals("999 B", formatBytes(999, en)); assertEquals("1.5 kB", formatBytes(1500, en))
        assertEquals("2.9 MB", formatBytes(2_900_000, en)); assertEquals("1.2 GB", formatBytes(1_200_000_000, en))
    }
    @Test fun megapixels() {
        assertEquals(12.0, megapixels(4000, 3000)!!, 0.001)
        assertEquals(null, megapixels(0, 100))
    }
}

class ClockTest {
    @Test fun clock() {
        assertEquals("0:00", formatClock(0)); assertEquals("0:09", formatClock(9_999))
        assertEquals("1:05", formatClock(65_000)); assertEquals("1:02:05", formatClock(3_725_000))
        assertEquals("0:00", formatClock(-5))
    }
    @Test fun seekClamps() {
        assertEquals(0L, seekTarget(4_000, 60_000, -10_000))
        assertEquals(60_000L, seekTarget(55_000, 60_000, SEEK_STEP_MS))
        assertEquals(24_000L, seekTarget(14_000, 60_000, SEEK_STEP_MS))
        assertEquals(30_000L, seekTarget(20_000, 0, SEEK_STEP_MS)) // duración desconocida
    }
    @Test fun progress() {
        assertEquals(0.5f, progressFraction(30, 60), 0f); assertEquals(0f, progressFraction(5, 0), 0f)
        assertEquals(1f, progressFraction(90, 60), 0f)
    }
}
