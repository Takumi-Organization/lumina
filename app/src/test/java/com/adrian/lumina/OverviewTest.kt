package com.adrian.lumina

import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.MediaKind
import com.adrian.lumina.domain.usecase.buildTimeline
import com.adrian.lumina.domain.usecase.overview
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverviewTest {
    private val utc = ZoneId.of("UTC")
    private fun item(id: Long, date: String) = MediaItem(
        id, "u$id", "$id.jpg", MediaKind.IMAGE, LocalDate.parse(date).atTime(12, 0).atZone(utc).toInstant().toEpochMilli() - id,
        100, 100, 0, 1, "", "", false,
    )

    @Test fun groupsMonthsIntoYearsNewestFirstWithCoverAndEntryIndex() {
        val t = buildTimeline(listOf(item(1, "2024-03-09"), item(2, "2024-01-02"), item(3, "2024-01-02"), item(4, "2022-05-05")), utc)
        val o = t.overview()
        assertEquals(listOf(2024, 2022), o.map { it.year })
        assertEquals(listOf(3, 1), o.map { it.count })
        assertEquals(listOf(YearMonth.of(2024, 3), YearMonth.of(2024, 1)), o[0].months.map { it.month })
        assertEquals(1L, o[0].cover.id)               // la más reciente del año
        assertEquals(t.months.map { it.entryIndex }, o.flatMap { y -> y.months.map { it.entryIndex } })
    }

    @Test fun emptyTimelineHasNoOverview() = assertTrue(com.adrian.lumina.domain.model.Timeline.Empty.overview().isEmpty())
}
