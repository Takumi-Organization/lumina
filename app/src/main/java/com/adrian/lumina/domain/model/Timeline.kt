package com.adrian.lumina.domain.model

import java.time.LocalDate
import java.time.YearMonth

/** Entrada plana de la cuadrícula cronológica: cabecera de día o miniatura. */
sealed interface TimelineEntry {
    val key: String

    data class Header(val day: LocalDate, val ids: List<Long>) : TimelineEntry {
        val count get() = ids.size
        override val key get() = "h$day"
    }

    data class Cell(val item: MediaItem) : TimelineEntry {
        override val key get() = "m${item.id}"
    }
}

/** Posición (índice de entrada) del primer elemento de cada mes, para el scrubber. */
data class MonthMark(val month: YearMonth, val entryIndex: Int, val count: Int)

data class Timeline(
    val entries: List<TimelineEntry>,
    val months: List<MonthMark>,
) {
    companion object {
        val Empty = Timeline(emptyList(), emptyList())
    }
}
