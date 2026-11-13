package com.adrian.lumina.editor

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Render no destructivo: [apply] produce una imagen nueva a partir de la original y un [EditState].
 * Vista previa y exportación usan este mismo código (con distinta resolución de entrada).
 */
object EditPipeline {

    fun apply(src: PixelImage, s: EditState): PixelImage {
        if (s.isIdentity) return src
        var img = crop(src, s.crop)
        img = orient(img, s.quarterTurns, s.flipHorizontal)
        if (hasTone(s)) img = tone(img, s)
        if (s.sharpen > 0f) img = sharpen(img, s.sharpen)
        if (s.vignette > 0f) img = vignette(img, s.vignette)
        return img
    }

    private fun hasTone(s: EditState) =
        s.exposure != 0f || s.contrast != 0f || s.saturation != 0f || s.warmth != 0f || s.shadows != 0f || s.highlights != 0f

    fun crop(src: PixelImage, c: CropRect): PixelImage {
        if (c.isFull) return src
        val l = (c.left.coerceIn(0f, 1f) * src.width).roundToInt().coerceIn(0, src.width - 1)
        val t = (c.top.coerceIn(0f, 1f) * src.height).roundToInt().coerceIn(0, src.height - 1)
        val r = (c.right.coerceIn(0f, 1f) * src.width).roundToInt().coerceIn(l + 1, src.width)
        val b = (c.bottom.coerceIn(0f, 1f) * src.height).roundToInt().coerceIn(t + 1, src.height)
        val w = r - l
        val h = b - t
        val out = IntArray(w * h)
        for (y in 0 until h) System.arraycopy(src.pixels, (y + t) * src.width + l, out, y * w, w)
        return PixelImage(w, h, out)
    }

    /** Espejo horizontal (antes del giro) y [turns] cuartos de vuelta horarios. */
    fun orient(src: PixelImage, turns: Int, flipH: Boolean): PixelImage {
        val q = ((turns % 4) + 4) % 4
        if (q == 0 && !flipH) return src
        val w = src.width
        val h = src.height
        val ow = if (q % 2 == 0) w else h
        val oh = if (q % 2 == 0) h else w
        val out = IntArray(ow * oh)
        for (y in 0 until h) for (x in 0 until w) {
            val sx = if (flipH) w - 1 - x else x
            val p = src.pixels[y * w + sx]
            // Destino de (x, y) tras q giros horarios.
            val (dx, dy) = when (q) {
                0 -> x to y
                1 -> (h - 1 - y) to x
                2 -> (w - 1 - x) to (h - 1 - y)
                else -> y to (w - 1 - x)
            }
            out[dy * ow + dx] = p
        }
        return PixelImage(ow, oh, out)
    }

    private fun clamp01(v: Float) = if (v < 0f) 0f else if (v > 1f) 1f else v

    fun tone(src: PixelImage, s: EditState): PixelImage {
        val gain = 2f.pow(s.exposure)
        val cf = 1f + s.contrast
        val sat = 1f + s.saturation
        val out = IntArray(src.pixels.size)
        for (i in src.pixels.indices) {
            val p = src.pixels[i]
            var r = ((p shr 16) and 0xFF) / 255f
            var g = ((p shr 8) and 0xFF) / 255f
            var b = (p and 0xFF) / 255f
            r *= gain; g *= gain; b *= gain
            // Sombras/luces: actúan según la luminosidad actual, con caída cuadrática hacia el extremo contrario.
            val lum0 = clamp01(0.2126f * r + 0.7152f * g + 0.0722f * b)
            val lift = s.shadows * 0.35f * (1f - lum0) * (1f - lum0) + s.highlights * 0.35f * lum0 * lum0
            r += lift; g += lift; b += lift
            r = (r - 0.5f) * cf + 0.5f; g = (g - 0.5f) * cf + 0.5f; b = (b - 0.5f) * cf + 0.5f
            val lum = 0.2126f * r + 0.7152f * g + 0.0722f * b
            r = lum + (r - lum) * sat; g = lum + (g - lum) * sat; b = lum + (b - lum) * sat
            r += s.warmth * 0.12f; b -= s.warmth * 0.12f
            out[i] = (p and 0xFF000000.toInt()) or
                ((clamp01(r) * 255f + 0.5f).toInt() shl 16) or ((clamp01(g) * 255f + 0.5f).toInt() shl 8) or (clamp01(b) * 255f + 0.5f).toInt()
        }
        return PixelImage(src.width, src.height, out)
    }

    /** Máscara de desenfoque 3×3 (bordes replicados): salida = original + cantidad·(original − desenfoque). */
    fun sharpen(src: PixelImage, amount: Float): PixelImage {
        val w = src.width
        val h = src.height
        val out = IntArray(src.pixels.size)
        for (y in 0 until h) for (x in 0 until w) {
            var acc = 0
            for (shift in SHIFTS) {
                var sum = 0
                for (dy in -1..1) for (dx in -1..1) {
                    val yy = (y + dy).coerceIn(0, h - 1)
                    val xx = (x + dx).coerceIn(0, w - 1)
                    sum += (src.pixels[yy * w + xx] shr shift) and 0xFF
                }
                val orig = (src.pixels[y * w + x] shr shift) and 0xFF
                acc = acc or ((orig + amount * (orig - sum / 9f)).roundToInt().coerceIn(0, 255) shl shift)
            }
            out[y * w + x] = (src.pixels[y * w + x] and 0xFF000000.toInt()) or acc
        }
        return PixelImage(w, h, out)
    }

    private val SHIFTS = intArrayOf(16, 8, 0)

    /** Oscurece hacia las esquinas: nada en el centro, hasta [amount] en la esquina. */
    fun vignette(src: PixelImage, amount: Float): PixelImage {
        val w = src.width
        val h = src.height
        val cx = (w - 1) / 2f
        val cy = (h - 1) / 2f
        val maxD = sqrt(cx * cx + cy * cy).coerceAtLeast(1f)
        val out = IntArray(src.pixels.size)
        for (y in 0 until h) for (x in 0 until w) {
            val dx = x - cx; val dy = y - cy
            val d = sqrt(dx * dx + dy * dy) / maxD          // 0 centro … 1 esquina
            val t = ((d - 0.35f) / 0.65f).coerceIn(0f, 1f)  // el 35 % central queda intacto
            val f = 1f - amount * t * t
            val p = src.pixels[y * w + x]
            val r = (((p shr 16) and 0xFF) * f + 0.5f).toInt()
            val g = (((p shr 8) and 0xFF) * f + 0.5f).toInt()
            val b = ((p and 0xFF) * f + 0.5f).toInt()
            out[y * w + x] = (p and 0xFF000000.toInt()) or (r shl 16) or (g shl 8) or b
        }
        return PixelImage(w, h, out)
    }
}
