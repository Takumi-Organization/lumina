package com.adrian.lumina

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adrian.lumina.data.db.LuminaDatabase
import com.adrian.lumina.data.db.RoomAlbums
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomAlbumsTest {
    private lateinit var db: LuminaDatabase
    private lateinit var repo: RoomAlbums
    private var clock = 1000L

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LuminaDatabase::class.java).build()
        repo = RoomAlbums(db.albums()) { clock++ }
    }
    @After fun tearDown() = db.close()

    private suspend fun albums() = repo.observeAlbums().first()

    @Test fun createRenameDescribeAndDelete() = runBlocking {
        val id = repo.create("  Viaje  ", " Córdoba ")
        assertEquals("Viaje", albums().single().name); assertEquals("Córdoba", albums().single().description)
        repo.update(id, "Viaje 2026", "")
        assertEquals("Viaje 2026", albums().single().name)
        repo.delete(id)
        assertTrue(albums().isEmpty())
    }

    @Test fun addKeepsOrderIgnoresDuplicatesAndRemoves() = runBlocking {
        val id = repo.create("A")
        repo.addItems(id, listOf(30, 10, 20))
        repo.addItems(id, listOf(10, 40)) // 10 ya está: no se duplica ni se mueve
        assertEquals(listOf(30L, 10L, 20L, 40L), albums().single().mediaIds)
        repo.removeItems(id, listOf(10, 99))
        assertEquals(listOf(30L, 20L, 40L), albums().single().mediaIds)
    }

    @Test fun reorderPersists() = runBlocking {
        val id = repo.create("A")
        repo.addItems(id, listOf(1, 2, 3, 4))
        repo.reorder(id, listOf(4, 2, 1, 3))
        assertEquals(listOf(4L, 2L, 1L, 3L), albums().single().mediaIds)
        repo.addItems(id, listOf(5)) // se añade al final tras reordenar
        assertEquals(listOf(4L, 2L, 1L, 3L, 5L), albums().single().mediaIds)
    }

    @Test fun favoriteHiddenCoverAndOrderingOfAlbums() = runBlocking {
        val a = repo.create("Primero"); val b = repo.create("Segundo"); val c = repo.create("Tercero")
        assertEquals(listOf("Tercero", "Segundo", "Primero"), albums().map { it.name }) // más reciente primero
        repo.setFavorite(a, true)
        assertEquals("Primero", albums().first().name)                                    // favoritos arriba
        repo.setHidden(b, true); repo.setCover(c, 77)
        val byName = albums().associateBy { it.name }
        assertTrue(byName.getValue("Segundo").isHidden); assertFalse(byName.getValue("Primero").isHidden)
        assertEquals(77L, byName.getValue("Tercero").coverMediaId)
        repo.setCover(c, null); assertNull(albums().first { it.id == c }.coverMediaId)
    }

    @Test fun largeRemovalIsChunked() = runBlocking {
        val id = repo.create("Grande")
        val ids = (1L..1300L).toList()
        repo.addItems(id, ids)
        assertEquals(1300, albums().single().mediaIds.size)
        repo.removeItems(id, ids)
        assertEquals(0, albums().single().mediaIds.size)
    }
}
