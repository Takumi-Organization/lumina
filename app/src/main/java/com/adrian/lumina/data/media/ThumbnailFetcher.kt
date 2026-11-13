package com.adrian.lumina.data.media

import android.content.ContentResolver
import android.net.Uri
import android.util.Size
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.Options

/** Petición de miniatura: nunca decodifica el original completo. */
data class Thumb(val uri: String, val sizePx: Int = 320)

class ThumbKeyer : Keyer<Thumb> {
    override fun key(data: Thumb, options: Options) = "thumb:${data.uri}:${data.sizePx}"
}

/** Usa el almacén de miniaturas del sistema (`loadThumbnail`), que suele estar ya en disco. */
class ThumbFetcher(
    private val data: Thumb,
    private val resolver: ContentResolver,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val bmp = resolver.loadThumbnail(Uri.parse(data.uri), Size(data.sizePx, data.sizePx), null)
        return ImageFetchResult(bmp.asImage(), isSampled = true, dataSource = DataSource.DISK)
    }

    class Factory(private val resolver: ContentResolver) : Fetcher.Factory<Thumb> {
        override fun create(data: Thumb, options: Options, imageLoader: ImageLoader): Fetcher =
            ThumbFetcher(data, resolver)
    }
}
