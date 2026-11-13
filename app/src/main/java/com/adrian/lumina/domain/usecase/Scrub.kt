package com.adrian.lumina.domain.usecase

import com.adrian.lumina.domain.model.MonthMark
import com.adrian.lumina.domain.model.Timeline
import kotlin.math.roundToInt

/** Mes al que pertenece una entrada de la cuadrícula (el último mes cuyo inicio es ≤ índice). */
fun Timeline.monthAtEntry(index: Int): MonthMark? =
    months.lastOrNull { it.entryIndex <= index } ?: months.firstOrNull()

/** Posición relativa (0..1) de un índice de entrada en el carril del scrubber. */
fun Timeline.fractionOfEntry(index: Int): Float =
    if (entries.size <= 1) 0f else (index.toFloat() / (entries.size - 1)).coerceIn(0f, 1f)

/** Entrada a la que saltar para una posición 0..1 del carril. */
fun Timeline.entryAtFraction(fraction: Float): Int =
    if (entries.isEmpty()) 0 else (fraction.coerceIn(0f, 1f) * (entries.size - 1)).roundToInt()

data class YearTick(val year: Int, val fraction: Float)

/** Una marca por año, en el primer mes (el más reciente) de cada año. */
fun Timeline.yearTicks(): List<YearTick> =
    months.distinctBy { it.month.year }.map { YearTick(it.month.year, fractionOfEntry(it.entryIndex)) }

/** Alterna un grupo de ids: si todos estaban seleccionados los quita; si no, los añade todos. */
fun Set<Long>.toggleGroup(ids: Collection<Long>): Set<Long> =
    if (ids.isNotEmpty() && containsAll(ids)) this - ids.toSet() else this + ids
