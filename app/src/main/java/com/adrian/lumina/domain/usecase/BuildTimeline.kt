package com.adrian.lumina.domain.usecase

import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.MonthMark
import com.adrian.lumina.domain.model.Timeline
import com.adrian.lumina.domain.model.TimelineEntry
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Agrupa por día (más reciente primero) y construye el índice de meses.
 * Pura y determinista: la zona horaria se inyecta para poder probarla.
 */
fun buildTimeline(items: List<MediaItem>, zone: ZoneId = ZoneId.systemDefault()): Timeline {
    if (items.isEmpty()) return Timeline.Empty
    val byDay = items
        .groupBy { Instant.ofEpochMilli(it.dateMillis).atZone(zone).toLocalDate() }
        .toSortedMap(compareByDescending<LocalDate> { it })

    val entries = ArrayList<TimelineEntry>(items.size + byDay.size)
    val months = ArrayList<MonthMark>()
    for ((day, group) in byDay) {
        val month = YearMonth.from(day)
        if (months.lastOrNull()?.month != month) {
            months += MonthMark(month, entries.size, 0)
        }
        months[months.lastIndex] = months.last().let { it.copy(count = it.count + group.size) }
        entries += TimelineEntry.Header(day, group.size)
        group.sortedByDescending { it.dateMillis }.forEach { entries += TimelineEntry.Cell(it) }
    }
    return Timeline(entries, months)
}
