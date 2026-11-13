package com.adrian.lumina.domain.usecase

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

private fun trim(d: Double, decimals: Int): String =
    String.format(Locale.ROOT, "%.${decimals}f", d).trimEnd('0').trimEnd('.')

/** 0.004 → «1/250 s»; 2.0 → «2 s»; 0.5 → «1/2 s». */
fun formatExposure(seconds: Double): String =
    if (seconds >= 1.0) "${trim(seconds, 1)} s" else "1/${(1.0 / seconds).roundToLong()} s"

/** 1.8 → «f/1.8»; 2.0 → «f/2». */
fun formatAperture(f: Double): String = "f/${trim(f, 1)}"

fun formatFocalLength(mm: Double): String = "${trim(mm, 1)} mm"

/** «40.4168° N, 3.7038° W»; independiente del idioma del sistema. */
fun formatCoords(lat: Double, lon: Double): String =
    String.format(Locale.ROOT, "%.4f° %s, %.4f° %s", abs(lat), if (lat >= 0) "N" else "S", abs(lon), if (lon >= 0) "E" else "W")

/** Tamaño legible con unidades decimales (1 MB = 1 000 000 B), como muestra Android. */
fun formatBytes(bytes: Long, locale: Locale = Locale.getDefault()): String {
    if (bytes < 1000) return "$bytes B"
    val units = arrayOf("kB", "MB", "GB", "TB")
    var v = bytes / 1000.0
    var i = 0
    while (v >= 1000 && i < units.lastIndex) { v /= 1000; i++ }
    return String.format(locale, "%.1f %s", v, units[i])
}

/** Megapíxeles con un decimal, o null si no hay dimensiones. */
fun megapixels(width: Int, height: Int): Double? =
    if (width <= 0 || height <= 0) null else width.toLong() * height / 1_000_000.0
