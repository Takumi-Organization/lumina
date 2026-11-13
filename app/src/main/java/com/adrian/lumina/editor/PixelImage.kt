package com.adrian.lumina.editor

/** Imagen ARGB_8888 en un IntArray (fila a fila). Sin Android: el mismo código sirve para vista previa, exportación y tests. */
class PixelImage(val width: Int, val height: Int, val pixels: IntArray) {
    init { require(pixels.size == width * height) { "pixels (${pixels.size}) != ${width}x$height" } }
    operator fun get(x: Int, y: Int) = pixels[y * width + x]
    override fun equals(other: Any?) = other is PixelImage && width == other.width && height == other.height && pixels.contentEquals(other.pixels)
    override fun hashCode() = 31 * (31 * width + height) + pixels.contentHashCode()
}
