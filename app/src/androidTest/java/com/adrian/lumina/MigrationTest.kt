package com.adrian.lumina

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.adrian.lumina.data.db.LuminaDatabase
import com.adrian.lumina.data.db.MIGRATION_1_2
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), LuminaDatabase::class.java)

    @Test fun v1ToV2KeepsFavoritesAndCreatesAlbumTables() {
        helper.createDatabase("mig-test", 1).apply {
            execSQL("INSERT INTO favorite (mediaId) VALUES (11), (22)")
            close()
        }
        val db = helper.runMigrationsAndValidate("mig-test", 2, true, MIGRATION_1_2) // valida el esquema contra 2.json
        db.query("SELECT mediaId FROM favorite ORDER BY mediaId").use { c ->
            val ids = buildList { while (c.moveToNext()) add(c.getLong(0)) }
            assertEquals(listOf(11L, 22L), ids)
        }
        db.execSQL("INSERT INTO album (name, description, isFavorite, isHidden, createdAt) VALUES ('A', '', 0, 0, 1)")
        db.execSQL("INSERT INTO album_item (albumId, mediaId, position) VALUES (1, 5, 0)")
        db.execSQL("DELETE FROM album WHERE id = 1")
        db.query("SELECT COUNT(*) FROM album_item").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) } // cascada
    }
}
