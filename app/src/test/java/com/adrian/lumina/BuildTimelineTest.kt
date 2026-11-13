package com.adrian.lumina

import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.MediaKind
import com.adrian.lumina.domain.model.TimelineEntry
import com.adrian.lumina.domain.usecase.buildTimeline
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
