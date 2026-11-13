package com.adrian.lumina.data.media

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.provider.MediaStore
import com.adrian.lumina.domain.model.MediaItem

/** Acciones del sistema sobre medios: compartir, papelera y reproducir fuera de la app. */
class MediaActions(private val context: Context) {

    fun shareChooser(items: List<MediaItem>): Intent {
        val uris = items.map { Uri.parse(it.uri) }
        val type = when {
            items.all { it.isVideo } -> "video/*"
            items.none { it.isVideo } -> "image/*"
            else -> "*/*"
        }
        val send = if (uris.size == 1) Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
        else Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        send.setType(type)
        send.clipData = ClipData.newRawUri(null, uris[0]).also { clip -> uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) } }
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** Pide al sistema mover a la papelera (recuperable); el sistema muestra su confirmación. */
    fun trashRequest(items: List<MediaItem>): IntentSender =
        MediaStore.createTrashRequest(context.contentResolver, items.map { Uri.parse(it.uri) }, true).intentSender
}
