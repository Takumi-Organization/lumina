package com.adrian.lumina.domain.model

import java.time.LocalDate
import java.time.YearMonth

/** Miniatura dentro de una fila del mosaico; [aspect] (ancho/alto, acotado) es también su peso horizontal en la fila. */
data class MosaicCell(val item: MediaItem, val aspect: Float)

/** Entrada plana de la biblioteca: cabecera de mes, marca de día o fila de miniaturas del mosaico. */
sealed interface TimelineEntry {
    val key: String

    /** Cabecera gigante de mes. */
    data class Month(val month: YearMonth, val count: Int) : TimelineEntry {
        override val key get() = "M$month"
    }

    /** Marca de día; sirve también para seleccionar el día entero. */
    data class Day(val day: LocalDate, val ids: List<Long>) : TimelineEntry {
        val count get() = ids.size
        override val key get() = "d$day"
    }

    /**
     * Fila del mosaico. Su alto es ancho/[aspect]; cada celda ocupa [MosaicCell.aspect] de ese ancho relativo y lo que
     * sobra ([aspect] menos la suma) se deja en blanco (última fila de un día). [hero]: primera foto del día, a todo ancho.
     */
    data class Row(val cells: List<MosaicCell>, val aspect: Float, val hero: Boolean, val day: LocalDate = LocalDate.EPOCH) : TimelineEntry {
        override val key get() = "r${cells.first().item.id}"
        val leftover get() = (aspect - cells.sumOf { it.aspect.toDouble() }.toFloat()).coerceAtLeast(0f)
    }
}

/** Posición (índice de entrada) de la cabecera de cada mes, para el scrubber. */
data class MonthMark(val month: YearMonth, val entryIndex: Int, val count: Int)

data class Timeline(
    val entries: List<TimelineEntry>,
    val months: List<MonthMark>,
) {
    /** Todos los medios, en el orden en que se ven (para el visor). */
    val items: List<MediaItem> by lazy(LazyThreadSafetyMode.NONE) {
        entries.filterIsInstance<TimelineEntry.Row>().flatMap { r -> r.cells.map { it.item } }
    }

    companion object {
        val Empty = Timeline(emptyList(), emptyList())
    }
}
