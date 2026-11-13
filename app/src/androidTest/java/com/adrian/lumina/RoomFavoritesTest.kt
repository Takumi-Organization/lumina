package com.adrian.lumina

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adrian.lumina.data.db.LuminaDatabase
import com.adrian.lumina.data.db.RoomFavorites
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomFavoritesTest {
    private lateinit var db: LuminaDatabase
    private lateinit var repo: RoomFavorites

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LuminaDatabase::class.java).build()
        repo = RoomFavorites(db.favorites())
    }
    @After fun tearDown() = db.close()

    @Test fun addRemoveAndIdempotent() = runBlocking {
        repo.setFavorite(listOf(1, 2, 3), true)
        repo.setFavorite(listOf(2, 3), true) // repetir no falla
        assertEquals(setOf(1L, 2L, 3L), repo.observeIds().first())
        repo.setFavorite(listOf(2), false)
        assertEquals(setOf(1L, 3L), repo.observeIds().first())
    }

    @Test fun largeBatchesAreChunked() = runBlocking {
        val ids = (1L..1700L).toList() // > límite de variables si no se parte en lotes
        repo.setFavorite(ids, true)
        assertEquals(1700, repo.observeIds().first().size)
        repo.setFavorite(ids, false)
        assertEquals(0, repo.observeIds().first().size)
    }
}
