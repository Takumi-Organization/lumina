package com.adrian.lumina.editor

/** Zona de recorte normalizada (0..1) sobre la imagen ya girada/espejada, tal como se ve. */
data class CropRect(val left: Float = 0f, val top: Float = 0f, val right: Float = 1f, val bottom: Float = 1f) {
    val isFull get() = left <= 0f && top <= 0f && right >= 1f && bottom >= 1f
    val width get() = right - left
    val height get() = bottom - top

    /** El mismo contenido tras un cuarto de vuelta horario de la imagen. */
    fun rotatedClockwise() = CropRect(1f - bottom, left, 1f - top, right)

    /** El mismo contenido tras un espejo horizontal. */
    fun mirrored() = CropRect(1f - right, top, 1f - left, bottom)
}

/** Agarres del recorte libre: ocho bordes/esquinas y el cuerpo (mover). */
enum class CropHandle { TOP_LEFT, TOP, TOP_RIGHT, LEFT, RIGHT, BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT, MOVE }

/** Tamaño mínimo del recorte (fracción del lado) para que nunca se colapse. */
const val MIN_CROP = 0.1f

/** Qué agarre está bajo el punto normalizado (x, y); [slop] es el radio de toque. null = fuera. */
fun hitHandle(r: CropRect, x: Float, y: Float, slop: Float = 0.06f): CropHandle? {
    val nearL = kotlin.math.abs(x - r.left) <= slop
    val nearR = kotlin.math.abs(x - r.right) <= slop
    val nearT = kotlin.math.abs(y - r.top) <= slop
    val nearB = kotlin.math.abs(y - r.bottom) <= slop
    val inX = x >= r.left - slop && x <= r.right + slop
    val inY = y >= r.top - slop && y <= r.bottom + slop
    return when {
        nearT && nearL -> CropHandle.TOP_LEFT
        nearT && nearR -> CropHandle.TOP_RIGHT
        nearB && nearL -> CropHandle.BOTTOM_LEFT
        nearB && nearR -> CropHandle.BOTTOM_RIGHT
        nearT && inX -> CropHandle.TOP
        nearB && inX -> CropHandle.BOTTOM
        nearL && inY -> CropHandle.LEFT
        nearR && inY -> CropHandle.RIGHT
        x > r.left && x < r.right && y > r.top && y < r.bottom -> CropHandle.MOVE
        else -> null
    }
}

/** Arrastra un agarre (dx, dy normalizados). El resultado siempre queda dentro de 0..1 y con tamaño ≥ [MIN_CROP]. */
fun dragCrop(r: CropRect, handle: CropHandle, dx: Float, dy: Float): CropRect {
    if (handle == CropHandle.MOVE) {
        val mx = dx.coerceIn(-r.left, 1f - r.right)
        val my = dy.coerceIn(-r.top, 1f - r.bottom)
        return CropRect(r.left + mx, r.top + my, r.right + mx, r.bottom + my)
    }
    var l = r.left; var t = r.top; var rt = r.right; var b = r.bottom
    when (handle) {
        CropHandle.TOP_LEFT, CropHandle.LEFT, CropHandle.BOTTOM_LEFT -> l = (l + dx).coerceIn(0f, rt - MIN_CROP)
        CropHandle.TOP_RIGHT, CropHandle.RIGHT, CropHandle.BOTTOM_RIGHT -> rt = (rt + dx).coerceIn(l + MIN_CROP, 1f)
        else -> {}
    }
    when (handle) {
        CropHandle.TOP_LEFT, CropHandle.TOP, CropHandle.TOP_RIGHT -> t = (t + dy).coerceIn(0f, b - MIN_CROP)
        CropHandle.BOTTOM_LEFT, CropHandle.BOTTOM, CropHandle.BOTTOM_RIGHT -> b = (b + dy).coerceIn(t + MIN_CROP, 1f)
        else -> {}
    }
    return CropRect(l, t, rt, b)
}

/**
 * Parámetros de edición (la imagen original nunca se toca). Los ajustes van de -1 a 1 salvo [sharpen] y [vignette] (0..1).
 * Orden de aplicación: giro/espejo → recorte (sobre la imagen ya girada) → tono → nitidez → viñeta.
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
