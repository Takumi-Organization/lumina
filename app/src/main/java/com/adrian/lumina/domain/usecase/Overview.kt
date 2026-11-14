package com.adrian.lumina.domain.usecase

import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.Timeline
import com.adrian.lumina.domain.model.TimelineEntry
import java.time.YearMonth

data class MonthOverview(val month: YearMonth, val count: Int, val cover: MediaItem, val entryIndex: Int)
data class YearOverview(val year: Int, val count: Int, val cover: MediaItem, val months: List<MonthOverview>)

/** Nivel de zoom de la biblioteca, como en Apple Fotos: años ⇢ meses ⇢ días. */
enum class ZoomLevel { YEARS, MONTHS, DAYS }

/** Resumen por año y mes (con portada = la foto más reciente del periodo) para los niveles de zoom gruesos. */
fun Timeline.overview(): List<YearOverview> {
    val months = ArrayList<MonthOverview>()
    var i = 0
    while (i < entries.size) {
        val e = entries[i]
        if (e is TimelineEntry.Month) {
            val cover = (i + 1 until entries.size).asSequence().map { entries[it] }.filterIsInstance<TimelineEntry.Row>().firstOrNull()?.cells?.firstOrNull()?.item
            if (cover != null) months += MonthOverview(e.month, e.count, cover, i)
        }
        i++
    }
    return months.groupBy { it.month.year }.map { (year, ms) ->
        YearOverview(year, ms.sumOf { it.count }, ms.first().cover, ms)
    }.sortedByDescending { it.year }
}
