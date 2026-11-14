package com.adrian.lumina.domain.usecase

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Color en HSL: [h] 0..360, [s] y [l] 0..1. */
data class Hsl(val h: Float, val s: Float, val l: Float)

fun rgbToHsl(argb: Int): Hsl {
    val r = ((argb shr 16) and 0xFF) / 255f; val g = ((argb shr 8) and 0xFF) / 255f; val b = (argb and 0xFF) / 255f
    val mx = max(r, max(g, b)); val mn = min(r, min(g, b))
    val l = (mx + mn) / 2f
    val d = mx - mn
    if (d < 1e-6f) return Hsl(0f, 0f, l)
    val s = d / (1f - abs(2f * l - 1f))
    val h = when (mx) {
        r -> 60f * (((g - b) / d) % 6f)
        g -> 60f * ((b - r) / d + 2f)
        else -> 60f * ((r - g) / d + 4f)
    }
    return Hsl(if (h < 0) h + 360f else h, s.coerceIn(0f, 1f), l)
}

fun hslToRgb(c: Hsl): Int {
    val k = (1f - abs(2f * c.l - 1f)) * c.s
    val h = (c.h % 360f) / 60f
    val x = k * (1f - abs(h % 2f - 1f))
    val (r1, g1, b1) = when (h.toInt()) {
        0 -> Triple(k, x, 0f); 1 -> Triple(x, k, 0f); 2 -> Triple(0f, k, x)
        3 -> Triple(0f, x, k); 4 -> Triple(x, 0f, k); else -> Triple(k, 0f, x)
    }
    val m = c.l - k / 2f
    fun ch(v: Float) = ((v + m).coerceIn(0f, 1f) * 255f + 0.5f).toInt()
    return (0xFF shl 24) or (ch(r1) shl 16) or (ch(g1) shl 8) or ch(b1)
}

/**
 * Color dominante de una miniatura (píxeles ARGB): media ponderada que prefiere los colores saturados y de
 * luminosidad media, para que un cielo azul gane a un borde negro o a un muro gris.
 */
fun dominantColor(pixels: IntArray): Int {
    if (pixels.isEmpty()) return 0xFF3A3B44.toInt()
    var sw = 0.0; var sr = 0.0; var sg = 0.0; var sb = 0.0
    for (p in pixels) {
        val hsl = rgbToHsl(p)
        val w = (0.12 + hsl.s) * max(0.05f, 1f - abs(hsl.l - 0.5f) * 1.4f)
        sw += w; sr += w * ((p shr 16) and 0xFF); sg += w * ((p shr 8) and 0xFF); sb += w * (p and 0xFF)
    }
    return (0xFF shl 24) or ((sr / sw).toInt() shl 16) or ((sg / sw).toInt() shl 8) or (sb / sw).toInt()
}

/** Fondo del visor: el matiz de la foto, muy oscuro y contenido, para que la foto destaque sin chocar. */
fun backgroundTint(rgb: Int): Int { val h = rgbToHsl(rgb); return hslToRgb(Hsl(h.h, min(h.s, 0.55f) * 0.9f, 0.075f)) }

/** Resplandor suave que se dibuja sobre el fondo: el mismo matiz, más claro y vivo. */
fun glowTint(rgb: Int): Int { val h = rgbToHsl(rgb); return hslToRgb(Hsl(h.h, max(0.45f, min(h.s, 0.8f)), 0.38f)) }
