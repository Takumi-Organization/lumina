package com.adrian.lumina.shots

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.Options
import com.adrian.lumina.data.media.Thumb
import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.MediaKind
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/** Fotos sintéticas procedurales (paisajes abstractos) para capturas reproducibles, sin usar fotos reales de nadie. */
object FakePhotos {
    private val palettes = listOf(
        intArrayOf(0xFF1B2A41.toInt(), 0xFFE76F51.toInt(), 0xFFF4A261.toInt()), // atardecer
        intArrayOf(0xFF0B3C49.toInt(), 0xFF2A9D8F.toInt(), 0xFFE9C46A.toInt()), // costa
        intArrayOf(0xFF2B2D42.toInt(), 0xFF8D99AE.toInt(), 0xFFEDF2F4.toInt()), // niebla
        intArrayOf(0xFF3D2645.toInt(), 0xFFD1495B.toInt(), 0xFFEDAE49.toInt()), // ciudad
        intArrayOf(0xFF1D3B2A.toInt(), 0xFF6A994E.toInt(), 0xFFF2E8CF.toInt()), // bosque
        intArrayOf(0xFF102542.toInt(), 0xFF3A86FF.toInt(), 0xFFFFD166.toInt()), // noche
    )

    fun bitmap(seed: Int, size: Int = 360): Bitmap {
        val r = Random(seed)
        val pal = palettes[r.nextInt(palettes.size)]
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, 0f, size.toFloat(), pal[0], pal[1], Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), p)
        p.shader = null
        p.color = pal[2]; c.drawCircle(size * (0.2f + r.nextFloat() * 0.6f), size * (0.25f + r.nextFloat() * 0.2f), size * (0.07f + r.nextFloat() * 0.1f), p)
        for (i in 0 until 3) {   // colinas
            p.color = (0xFF000000.toInt() or ((pal[0] and 0xFFFFFF) shr 1 and 0x7F7F7F)) + (i * 0x0A0A0A)
            val y = size * (0.55f + i * 0.15f)
            val path = android.graphics.Path().apply {
                moveTo(0f, size.toFloat()); lineTo(0f, y)
                var x = 0f
                while (x < size) { x += size / 6f; lineTo(x, y - r.nextFloat() * size * 0.12f) }
                lineTo(size.toFloat(), size.toFloat()); close()
            }
            c.drawPath(path, p)
        }
        return bmp
    }

    private class FakeFetcher(private val data: Thumb) : Fetcher {
        override suspend fun fetch(): FetchResult =
            ImageFetchResult(bitmap(data.uri.hashCode()).asImage(), isSampled = true, dataSource = DataSource.MEMORY)
        class Factory : Fetcher.Factory<Thumb> {
            override fun create(data: Thumb, options: Options, imageLoader: ImageLoader): Fetcher = FakeFetcher(data)
        }
    }

    private class FakeKeyer : Keyer<Thumb> { override fun key(data: Thumb, options: Options) = "fake:${data.uri}" }

    fun install(context: Context) {
        SingletonImageLoader.setUnsafe(ImageLoader.Builder(context).components { add(FakeKeyer()); add(FakeFetcher.Factory()) }.build())
    }

    /** Biblioteca de [n] elementos a lo largo de los últimos meses, con proporciones variadas y algún vídeo. */
    fun library(n: Int, zone: ZoneId = ZoneId.of("UTC")): List<MediaItem> {
        val r = Random(7)
        val today = LocalDate.of(2026, 11, 13)
        val shapes = listOf(4000 to 3000, 3000 to 4000, 4000 to 3000, 1920 to 1080, 3000 to 3000, 4032 to 3024)
        var day = today
        var left = 0
        return List(n) { i ->
            if (left == 0) { day = day.minusDays(1L + r.nextInt(3)); left = 1 + r.nextInt(9) }
            left--
            val (w, h) = shapes[r.nextInt(shapes.size)]
            val video = i % 11 == 4
            MediaItem(
                i + 1L, "content://media/fake/$i", "IMG_$i.jpg", if (video) MediaKind.VIDEO else MediaKind.IMAGE,
                day.atTime(8 + r.nextInt(12), r.nextInt(60)).atZone(zone).toInstant().toEpochMilli() - i,
                w, h, if (video) 12_000L + r.nextInt(80_000) else 0, 1, "Camera", "DCIM/Camera/", i % 9 == 0,
            )
        }
    }
}
