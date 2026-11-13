package com.adrian.lumina

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adrian.lumina.domain.model.*
import com.adrian.lumina.domain.usecase.*
import com.adrian.lumina.ui.albums.AlbumDetail
import com.adrian.lumina.ui.albums.AlbumDetailActions
import com.adrian.lumina.ui.albums.AlbumDetailScreen
import com.adrian.lumina.ui.albums.AlbumsScreen
import com.adrian.lumina.ui.search.SearchScreen
import com.adrian.lumina.ui.search.SearchUi
import com.adrian.lumina.ui.settings.SettingsScreen
import com.adrian.lumina.ui.settings.StorageUi
import com.adrian.lumina.ui.theme.LuminaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Pantallas de álbumes, búsqueda y ajustes sin estado: interacción básica (sin MediaStore real). */
@RunWith(AndroidJUnit4::class)
class Phase5to8ScreensTest {
    @get:Rule val rule = createComposeRule()

    // Los textos dependen del idioma del emulador: se resuelven con los recursos reales.
    private fun str(id: Int) = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun m(id: Long, path: String = "DCIM/Camera/") =
        MediaItem(id, "content://media/none/$id", "IMG_$id.jpg", MediaKind.IMAGE, 1_700_000_000_000L - id, 10, 10, 0, 1, "Camera", path, false)

    @Test fun albumsScreenShowsSectionsAndOpens() {
        val items = (1L..4L).map { m(it) }
        val overview = albumOverview(items, listOf(Album(7, "Viaje", "", null, true, false, listOf(1, 2))), 1_700_000_000_000L, false)
        var opened: AlbumRef? = null
        rule.setContent { LuminaTheme { AlbumsScreen(overview, false, { opened = it }, {}, {}) } }
        rule.onNodeWithText("Viaje").assertIsDisplayed()
        rule.onNodeWithTag("album-${AlbumRef.Manual(7)}").performClick()
        assertEquals(AlbumRef.Manual(7), opened)
    }

    @Test fun albumDetailSelectionMenuOffersManualActions() {
        val album = Album(7, "Viaje", "Verano", null, false, false, listOf(1, 2, 3))
        var selection by mutableStateOf(setOf(2L))
        var moved = 0
        val detail = AlbumDetail(AlbumRef.Manual(7), album, listOf(m(1), m(2), m(3)))
        rule.setContent {
            LuminaTheme {
                AlbumDetailScreen(detail, selection, AlbumSort.MANUAL, AlbumDetailActions(onMoveSelected = { moved += it }))
            }
        }
        rule.onNodeWithTag("album-selection-menu").performClick()
        rule.onNodeWithText(str(R.string.album_move_later)).performClick()
        assertEquals(1, moved)
    }

    @Test fun searchTypingCallsBackAndShowsEmptyAndResults() {
        var q by mutableStateOf("")
        var state by mutableStateOf<SearchUi>(SearchUi.Idle)
        var opened = 0L
        rule.setContent { LuminaTheme { SearchScreen(q, state, { q = it }, { opened = it }) } }
        rule.onNodeWithTag("search-field").performTextInput("playa")
        assertEquals("playa", q)
        state = SearchUi.Results(emptyList())
        rule.onNodeWithText(str(R.string.search_none_title)).assertIsDisplayed()
        state = SearchUi.Results(listOf(m(5)))
        rule.onNodeWithTag("cell-5").performClick()
        assertEquals(5L, opened)
    }

    @Test fun settingsTogglesEmitUpdatesAndShowStorage() {
        var current = AppSettings()
        val items = listOf(m(1), m(2))
        rule.setContent {
            LuminaTheme {
                SettingsScreen(current, StorageUi(storageSummary(items), 3, 2), "0.1.0", { current = it(current) }, {})
            }
        }
        rule.onNodeWithTag("set-haptics").performClick()
        assertTrue(!current.haptics)
        rule.onNodeWithTag("set-theme-DARK").performClick()
        assertEquals(ThemeMode.DARK, current.theme)
        rule.onNodeWithTag("storage-photos").assertIsDisplayed()
    }
}
