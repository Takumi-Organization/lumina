package com.adrian.lumina

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.adrian.lumina.data.prefs.DataStoreSettings
import com.adrian.lumina.domain.model.*
import com.adrian.lumina.domain.usecase.storageSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsTest {
    @get:Rule val tmp = TemporaryFolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val file get() = tmp.root.resolve("settings.preferences_pb")
    private val store by lazy { PreferenceDataStoreFactory.create(scope = scope) { file } }
    private val repo by lazy { DataStoreSettings(store) }

    @After fun close() = scope.cancel()

    @Test fun defaultsWhenEmpty() = runBlocking { assertEquals(AppSettings(), repo.settings.first()) }

    @Test fun updatesPersistAndRoundTrip() = runBlocking {
        val custom = AppSettings(ThemeMode.DARK, 6, ThumbQuality.HIGH, true, false, false, AlbumSort.NAME, true)
        repo.update { custom }
        assertEquals(custom, repo.settings.first())
        // Un repositorio nuevo sobre el mismo archivo ve lo guardado (persistencia real en disco).
        scope.cancel()
        val scope2 = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val again = DataStoreSettings(PreferenceDataStoreFactory.create(scope = scope2) { file })
            assertEquals(custom, again.settings.first())
        } finally { scope2.cancel() }
    }

    @Test fun partialUpdateKeepsOtherFields() = runBlocking {
        repo.update { it.copy(theme = ThemeMode.LIGHT) }
        repo.update { it.copy(haptics = false) }
        val s = repo.settings.first()
        assertEquals(ThemeMode.LIGHT, s.theme); assertEquals(false, s.haptics); assertEquals(4, s.defaultColumns)
    }

    @Test fun outOfRangeAndUnknownValuesAreSanitized() = runBlocking {
        store.edit { it[intPreferencesKey("default_columns")] = 99; it[stringPreferencesKey("theme")] = "NEON" }
        val s = repo.settings.first()
        assertEquals(7, s.defaultColumns); assertEquals(ThemeMode.SYSTEM, s.theme)
        repo.update { it.copy(defaultColumns = 0) }
        assertEquals(2, repo.settings.first().defaultColumns)
    }

    @Test fun storageSummaryAddsBytesByKind() {
        fun m(id: Long, video: Boolean, size: Long) = MediaItem(id, "u", "n", if (video) MediaKind.VIDEO else MediaKind.IMAGE, 0, 1, 1, 0, size, "b", "p", false)
        val s = storageSummary(listOf(m(1, false, 100), m(2, false, 50), m(3, true, 1000)))
        assertEquals(2, s.photos); assertEquals(1, s.videos); assertEquals(150L, s.photoBytes); assertEquals(1150L, s.totalBytes); assertEquals(3, s.count)
        assertEquals(0L, storageSummary(emptyList()).totalBytes)
    }
}
