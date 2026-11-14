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
 * Agrupa por mes y día (más reciente primero), maqueta cada día como mosaico y construye el índice de meses.
 * Pura y determinista: la zona horaria se inyecta para poder probarla; [target] es la densidad del mosaico.
 */
fun buildTimeline(items: List<MediaItem>, zone: ZoneId = ZoneId.systemDefault(), target: Float = 3f): Timeline {
    if (items.isEmpty()) return Timeline.Empty
    val byDay = items
        .groupBy { Instant.ofEpochMilli(it.dateMillis).atZone(zone).toLocalDate() }
        .toSortedMap(compareByDescending<LocalDate> { it })

    val entries = ArrayList<TimelineEntry>(items.size / 2 + byDay.size * 2)
    val months = ArrayList<MonthMark>()
    var monthHeaderIndex = -1
    var monthCount = 0
    fun closeMonth() {
        if (monthHeaderIndex >= 0) {
            val m = entries[monthHeaderIndex] as TimelineEntry.Month
            entries[monthHeaderIndex] = m.copy(count = monthCount)
            months[months.lastIndex] = months.last().copy(count = monthCount)
        }
    }
    for ((day, group) in byDay) {
        val month = YearMonth.from(day)
        if (months.lastOrNull()?.month != month) {
            closeMonth()
            monthHeaderIndex = entries.size; monthCount = 0
            months += MonthMark(month, entries.size, 0)
            entries += TimelineEntry.Month(month, 0)
        }
        monthCount += group.size
        val sorted = group.sortedByDescending { it.dateMillis }
        entries += TimelineEntry.Day(day, sorted.map { it.id })
        entries += layoutMosaic(sorted, target, day)
    }
    closeMonth()
    return Timeline(entries, months)
}
