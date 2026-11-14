package com.adrian.lumina.domain.usecase

import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.MosaicCell
import com.adrian.lumina.domain.model.TimelineEntry
import com.adrian.lumina.domain.model.EPOCH_DAY
import java.time.LocalDate

private const val MIN_ASPECT = 0.62f
private const val MAX_ASPECT = 1.9f
/** Variación del largo de cada fila para que el mosaico no parezca una cuadrícula (determinista). */
private val ROW_RHYTHM = floatArrayOf(1.0f, 1.35f, 0.8f, 1.15f)

private fun aspectOf(i: MediaItem): Float =
    if (i.width > 0 && i.height > 0) (i.width.toFloat() / i.height).coerceIn(MIN_ASPECT, MAX_ASPECT) else 1f

/**
 * Mosaico editorial «justificado»: filas de altura variable cuyo ancho se reparte según la proporción real de cada foto.
 * [target] es la suma de proporciones por fila (≈ columnas · 0.75). Con densidad baja y ≥3 fotos, la primera del día va
 * sola a todo ancho (portada del día). Pura: mismo resultado para la misma entrada.
 */
fun layoutMosaic(items: List<MediaItem>, target: Float, day: LocalDate = EPOCH_DAY): List<TimelineEntry.Row> {
    if (items.isEmpty()) return emptyList()
    val rows = ArrayList<TimelineEntry.Row>()
    var start = 0
    if (items.size >= 3 && target >= 2.2f) {
        val a = aspectOf(items[0]).coerceIn(1.15f, 1.6f)
        rows += TimelineEntry.Row(listOf(MosaicCell(items[0], a)), a, hero = true, day = day)
        start = 1
    }
    var rowIndex = 0
    var current = ArrayList<MosaicCell>()
    var sum = 0f
    fun rowTarget() = target * ROW_RHYTHM[rowIndex % ROW_RHYTHM.size]
    fun close(fill: Boolean) {
        if (current.isEmpty()) return
        // Última fila corta: no se estira (quedaría enorme); se deja hueco a la derecha.
        val aspect = if (fill) sum else maxOf(sum, rowTarget() * 0.7f)
        rows += TimelineEntry.Row(current, aspect, hero = false, day = day)
        current = ArrayList(); sum = 0f; rowIndex++
    }
    for (k in start until items.size) {
        val a = aspectOf(items[k])
        if (current.isNotEmpty() && sum + a > rowTarget() * 1.3f) close(fill = true)
        current += MosaicCell(items[k], a); sum += a
        if (sum >= rowTarget()) close(fill = true)
    }
    close(fill = false)
    return rows
}
