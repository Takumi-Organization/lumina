package com.adrian.lumina

import com.adrian.lumina.domain.model.Album
import com.adrian.lumina.domain.model.AlbumRef
import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.MediaKind
import com.adrian.lumina.domain.model.SmartType
import com.adrian.lumina.domain.usecase.albumItems
import com.adrian.lumina.domain.usecase.albumOverview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumOverviewTest {
    private val now = 1_700_000_000_000L
    private fun m(id: Long, path: String = "DCIM/Camera/", ageDays: Long = 1) =
        MediaItem(id, "u$id", "$id.jpg", MediaKind.IMAGE, now - ageDays * 86_400_000L, 1, 1, 0, 1, "b", path, false)
    private val all = listOf(m(1), m(2, "Pictures/Memes/"), m(3, "Pictures/Memes/", 50))
    private fun album(id: Long, ids: List<Long>, cover: Long? = null, hidden: Boolean = false, fav: Boolean = false) =
        Album(id, "A$id", "", cover, fav, hidden, ids)

    @Test fun manualKeepsOrderAndSkipsMissingIds() {
        val a = album(1, listOf(3, 99, 1))
        assertEquals(listOf(3L, 1L), albumItems(AlbumRef.Manual(1), all, listOf(a), now).map { it.id })
    }

    @Test fun unknownManualAlbumIsEmpty() = assertEquals(emptyList<MediaItem>(), albumItems(AlbumRef.Manual(7), all, emptyList(), now))

    @Test fun folderAndSmartResolve() {
        assertEquals(listOf(2L, 3L), albumItems(AlbumRef.Folder("Pictures/Memes/"), all, emptyList(), now).map { it.id })
        assertEquals(listOf(1L), albumItems(AlbumRef.Smart(SmartType.CAMERA), all, emptyList(), now).map { it.id })
    }

    @Test fun coverIsChosenOrFirst() {
        val o = albumOverview(all, listOf(album(1, listOf(1, 2), cover = 2), album(2, listOf(3))), now, false)
        assertEquals(2L, o.manual[0].cover?.id)
        assertEquals(3L, o.manual[1].cover?.id)
    }

    @Test fun coverMissingFallsBackToFirstAndEmptyHasNone() {
        val o = albumOverview(all, listOf(album(1, listOf(1), cover = 99), album(2, emptyList())), now, false)
        assertEquals(1L, o.manual[0].cover?.id)
        assertNull(o.manual[1].cover); assertEquals(0, o.manual[1].count)
    }

    @Test fun hiddenOnlyWhenRequested() {
        val al = listOf(album(1, listOf(1)), album(2, listOf(2), hidden = true))
        assertEquals(listOf(1L), albumOverview(all, al, now, false).manual.map { (it.ref as AlbumRef.Manual).id })
        assertEquals(2, albumOverview(all, al, now, true).manual.size)
    }

    @Test fun emptySmartAlbumsAreOmitted() {
        val types = albumOverview(all, emptyList(), now, false).smart.map { (it.ref as AlbumRef.Smart).type }
        assertTrue(SmartType.VIDEOS !in types && SmartType.DOWNLOADS !in types)
        assertTrue(SmartType.CAMERA in types)
    }
}
