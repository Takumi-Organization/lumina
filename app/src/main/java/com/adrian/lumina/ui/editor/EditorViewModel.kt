package com.adrian.lumina.ui.editor

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adrian.lumina.data.media.ImageCodec
import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.editor.Adjustment
import com.adrian.lumina.editor.CropRect
import com.adrian.lumina.editor.EditPipeline
import com.adrian.lumina.editor.EditState
import com.adrian.lumina.editor.PixelImage
import com.adrian.lumina.editor.UndoStack
import com.adrian.lumina.editor.centeredCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface SaveState {
    data object Idle : SaveState
    data object Saving : SaveState
    data class Done(val uri: String) : SaveState
    data object Failed : SaveState
}

/** Lado mayor de la vista previa y de la exportación (la exportación es la «resolución completa» hasta 4096 px). */
const val PREVIEW_SIDE = 1280
const val EXPORT_SIDE = 4096

class EditorViewModel(private val item: MediaItem, private val codec: ImageCodec) : ViewModel() {

    private val history = MutableStateFlow(UndoStack(EditState()))
    /** Valor en vivo mientras se arrastra un deslizador; se confirma al soltar. */
    private val draft = MutableStateFlow<EditState?>(null)

    val live: StateFlow<EditState> = combine(history, draft) { h, d -> d ?: h.current }
        .stateIn(viewModelScope, SharingStarted.Eagerly, EditState())
    val canUndo = MutableStateFlow(false)
    val canRedo = MutableStateFlow(false)
    val original = MutableStateFlow<Bitmap?>(null)
    val preview = MutableStateFlow<Bitmap?>(null)
    val failedToLoad = MutableStateFlow(false)
    val saveState = MutableStateFlow<SaveState>(SaveState.Idle)

    private var source: PixelImage? = null

    init {
        viewModelScope.launch {
            val src = runCatching { withContext(Dispatchers.IO) { codec.decode(item.uri, PREVIEW_SIDE) } }.getOrNull()
            if (src == null) { failedToLoad.value = true; return@launch }
            source = src
            original.value = ImageCodec.toBitmap(src)
            combine(live, cropMode) { st, cm -> if (cm) st.copy(crop = CropRect()) else st }.collectLatest { st ->
                preview.value = withContext(Dispatchers.Default) { ImageCodec.toBitmap(EditPipeline.apply(src, st)) }
            }
        }
        viewModelScope.launch {
            history.collect { canUndo.value = it.canUndo; canRedo.value = it.canRedo }
        }
    }

    val hasChanges get() = !history.value.current.isIdentity

    fun adjust(a: Adjustment, value: Float) { draft.value = a.set(draft.value ?: history.value.current, value) }
    fun commitDraft() { draft.value?.let { history.value = history.value.push(it) }; draft.value = null }

    private fun apply(f: (EditState) -> EditState) { draft.value = null; history.value = history.value.push(f(history.value.current)) }
    /** Gira conservando el mismo contenido recortado (el rectángulo gira con la imagen). */
    fun rotate(turns: Int) = apply { st ->
        var crop = st.crop
        repeat(((turns % 4) + 4) % 4) { crop = crop.rotatedClockwise() }
        st.copy(quarterTurns = ((st.quarterTurns + turns) % 4 + 4) % 4, crop = crop)
    }
    fun flip() = apply { it.copy(flipHorizontal = !it.flipHorizontal, crop = it.crop.mirrored()) }

    /** Recorte libre: se actualiza en vivo al arrastrar y se confirma con [commitDraft]. */
    fun cropDraft(rect: CropRect) { draft.value = (draft.value ?: history.value.current).copy(crop = rect) }
    fun reset() = apply { EditState() }
    fun undo() { draft.value = null; history.value = history.value.undo() }
    fun redo() { draft.value = null; history.value = history.value.redo() }

    /** Recorte centrado con proporción ancho/alto de la imagen resultante ya girada; null = sin recorte. */
    fun cropRatio(ratio: Float?) {
        val src = source ?: return
        apply { st ->
            if (ratio == null) st.copy(crop = CropRect())
            else {
                val odd = st.quarterTurns % 2 == 1
                val w = if (odd) src.height else src.width
                val h = if (odd) src.width else src.height
                st.copy(crop = centeredCrop(w, h, ratio))
            }
        }
    }

    /** En la pestaña de recorte se muestra la imagen entera (con tono) para poder arrastrar el rectángulo. */
    val cropMode = MutableStateFlow(false)

    /** Renderiza a resolución completa (≤ [EXPORT_SIDE]) y guarda una copia nueva. */
    fun save() {
        if (saveState.value == SaveState.Saving) return
        saveState.value = SaveState.Saving
        val st = history.value.current
        viewModelScope.launch {
            saveState.value = runCatching {
                withContext(Dispatchers.IO) {
                    val full = codec.decode(item.uri, EXPORT_SIDE)
                    codec.saveCopy(EditPipeline.apply(full, st), item.name, item.uri).toString()
                }
            }.fold({ SaveState.Done(it) }, { android.util.Log.w("Lumina", "No se pudo guardar la copia", it); SaveState.Failed })
        }
    }

    fun acknowledgeSave() { saveState.value = SaveState.Idle }

    /** Libera los bitmaps al cerrar el editor (el ViewModel puede sobrevivir un rato en el almacén de la actividad). */
    fun release() { preview.value = null; original.value = null; source = null }
}
