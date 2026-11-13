package com.adrian.lumina.editor

/** Zona de recorte normalizada (0..1) sobre la imagen ya sin girar. */
data class CropRect(val left: Float = 0f, val top: Float = 0f, val right: Float = 1f, val bottom: Float = 1f) {
    val isFull get() = left <= 0f && top <= 0f && right >= 1f && bottom >= 1f
}

/**
 * Parámetros de edición (la imagen original nunca se toca). Los ajustes van de -1 a 1 salvo [sharpen] y [vignette] (0..1).
 * Orden de aplicación: recorte → giro/espejo → tono → nitidez → viñeta.
 */
data class EditState(
    val exposure: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val shadows: Float = 0f,
    val highlights: Float = 0f,
    val sharpen: Float = 0f,
    val vignette: Float = 0f,
    /** Cuartos de vuelta en sentido horario (0..3). */
    val quarterTurns: Int = 0,
    val flipHorizontal: Boolean = false,
    val crop: CropRect = CropRect(),
) {
    val isIdentity get() = this == EditState()
}

enum class Adjustment(val min: Float) {
    EXPOSURE(-1f), CONTRAST(-1f), SATURATION(-1f), WARMTH(-1f), SHADOWS(-1f), HIGHLIGHTS(-1f), SHARPEN(0f), VIGNETTE(0f);

    fun get(s: EditState) = when (this) {
        EXPOSURE -> s.exposure; CONTRAST -> s.contrast; SATURATION -> s.saturation; WARMTH -> s.warmth
        SHADOWS -> s.shadows; HIGHLIGHTS -> s.highlights; SHARPEN -> s.sharpen; VIGNETTE -> s.vignette
    }

    fun set(s: EditState, v: Float): EditState {
        val x = v.coerceIn(min, 1f)
        return when (this) {
            EXPOSURE -> s.copy(exposure = x); CONTRAST -> s.copy(contrast = x); SATURATION -> s.copy(saturation = x)
            WARMTH -> s.copy(warmth = x); SHADOWS -> s.copy(shadows = x); HIGHLIGHTS -> s.copy(highlights = x)
            SHARPEN -> s.copy(sharpen = x); VIGNETTE -> s.copy(vignette = x)
        }
    }
}

/** Recortes con proporción fija, centrados. [ratio] es ancho/alto de la imagen ya girada. */
fun centeredCrop(imageWidth: Int, imageHeight: Int, ratio: Float): CropRect {
    if (imageWidth <= 0 || imageHeight <= 0 || ratio <= 0f) return CropRect()
    val imageRatio = imageWidth.toFloat() / imageHeight
    return if (imageRatio > ratio) {
        val w = ratio / imageRatio
        CropRect((1 - w) / 2, 0f, (1 + w) / 2, 1f)
    } else {
        val h = imageRatio / ratio
        CropRect(0f, (1 - h) / 2, 1f, (1 + h) / 2)
    }
}

/** Historial acotado de estados para deshacer/rehacer. Inmutable: cada operación devuelve un historial nuevo. */
class UndoStack<T> private constructor(
    val current: T,
    private val past: List<T>,
    private val future: List<T>,
    private val limit: Int,
) {
    constructor(initial: T, limit: Int = 100) : this(initial, emptyList(), emptyList(), limit)

    val canUndo get() = past.isNotEmpty()
    val canRedo get() = future.isNotEmpty()

    /** Nuevo estado; borra el futuro. Si es igual al actual no añade historia. */
    fun push(value: T): UndoStack<T> =
        if (value == current) this else UndoStack(value, (past + current).takeLast(limit), emptyList(), limit)

    fun undo(): UndoStack<T> = if (!canUndo) this else UndoStack(past.last(), past.dropLast(1), listOf(current) + future, limit)
    fun redo(): UndoStack<T> = if (!canRedo) this else UndoStack(future.first(), past + current, future.drop(1), limit)
}
