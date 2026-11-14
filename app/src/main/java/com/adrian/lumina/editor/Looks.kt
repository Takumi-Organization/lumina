package com.adrian.lumina.editor

/** Looks con identidad propia: cada uno es solo un conjunto de parámetros de [EditState] (no destructivo y retocable). */
enum class Look { ORIGINAL, VIVID, WARM, MONO, CINE, FADE }

/** Aplica el look sobre la geometría actual (recorte, giro, espejo) sustituyendo solo los parámetros de tono. */
fun Look.applyTo(base: EditState): EditState {
    val neutral = base.copy(exposure = 0f, contrast = 0f, saturation = 0f, warmth = 0f, shadows = 0f, highlights = 0f, sharpen = 0f, vignette = 0f)
    return when (this) {
        Look.ORIGINAL -> neutral
        Look.VIVID -> neutral.copy(contrast = 0.18f, saturation = 0.35f, highlights = -0.1f, sharpen = 0.25f)
        Look.WARM -> neutral.copy(warmth = 0.4f, exposure = 0.05f, contrast = 0.08f, saturation = 0.1f, vignette = 0.15f)
        Look.MONO -> neutral.copy(saturation = -1f, contrast = 0.25f, highlights = -0.15f, vignette = 0.25f)
        Look.CINE -> neutral.copy(contrast = 0.22f, shadows = 0.22f, highlights = -0.2f, warmth = 0.12f, saturation = -0.12f, vignette = 0.4f)
        Look.FADE -> neutral.copy(contrast = -0.25f, shadows = 0.35f, saturation = -0.2f, exposure = 0.1f)
    }
}

/** Reducción por vecino más próximo (barata, suficiente para miniaturas de los looks). */
fun PixelImage.downscale(maxSide: Int): PixelImage {
    val longest = maxOf(width, height)
    if (longest <= maxSide) return this
    val f = maxSide.toFloat() / longest
    val w = (width * f).toInt().coerceAtLeast(1); val h = (height * f).toInt().coerceAtLeast(1)
    val out = IntArray(w * h)
    for (y in 0 until h) { val sy = (y / f).toInt().coerceAtMost(height - 1); for (x in 0 until w) out[y * w + x] = pixels[sy * width + (x / f).toInt().coerceAtMost(width - 1)] }
    return PixelImage(w, h, out)
}
