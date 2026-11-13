package com.adrian.lumina.domain.usecase

import java.util.Locale

/** 65 000 ms → «1:05»; 3 725 000 ms → «1:02:05». */
fun formatClock(ms: Long): String {
    val s = (ms.coerceAtLeast(0)) / 1000
    return if (s >= 3600) String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, s % 3600 / 60, s % 60)
    else String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60)
}

/** Posición destino de un salto relativo, acotada a [0, duración]. Con duración desconocida (≤0) solo se acota por abajo. */
fun seekTarget(positionMs: Long, durationMs: Long, deltaMs: Long): Long {
    val t = (positionMs + deltaMs).coerceAtLeast(0)
    return if (durationMs > 0) t.coerceAtMost(durationMs) else t
}

/** Fracción 0..1 de progreso; 0 si no se conoce la duración. */
fun progressFraction(positionMs: Long, durationMs: Long): Float =
    if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)

const val SEEK_STEP_MS = 10_000L
