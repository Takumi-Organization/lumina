package com.adrian.lumina.domain.repository

import com.adrian.lumina.domain.model.LibraryStatus
import com.adrian.lumina.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface MediaRepository {
    /** Emite la lista completa y vuelve a emitir cuando MediaStore cambia o se pide [refresh]. Nunca lanza: los fallos van a [status]. */
    fun observeAll(): Flow<List<MediaItem>>

    /** Resultado de la última lectura (error y detalle), para mostrarlo en la UI. */
    val status: StateFlow<LibraryStatus> get() = MutableStateFlow(LibraryStatus())

    /** Vuelve a leer MediaStore (al volver a la app, al cambiar los permisos, al pulsar «reintentar»). */
    fun refresh() {}
}
