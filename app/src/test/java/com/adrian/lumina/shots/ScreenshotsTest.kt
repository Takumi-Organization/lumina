package com.adrian.lumina.shots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.adrian.lumina.domain.model.Album
import com.adrian.lumina.domain.model.AppSettings
import com.adrian.lumina.domain.usecase.StorageSummary
import com.adrian.lumina.domain.usecase.ZoomLevel
import com.adrian.lumina.domain.usecase.albumOverview
import com.adrian.lumina.domain.usecase.buildTimeline
import com.adrian.lumina.domain.usecase.storageSummary
import com.adrian.lumina.ui.albums.AlbumsScreen
import com.adrian.lumina.ui.components.LuminaTabs
import com.adrian.lumina.ui.gallery.GalleryActions
import com.adrian.lumina.ui.gallery.GalleryScreen
import com.adrian.lumina.ui.gallery.GalleryUiState
import com.adrian.lumina.ui.onboarding.Onboarding
import com.adrian.lumina.ui.search.SearchScreen
import com.adrian.lumina.ui.search.SearchUi
import com.adrian.lumina.ui.settings.SettingsScreen
import com.adrian.lumina.ui.settings.StorageUi
import com.adrian.lumina.ui.theme.LuminaTheme
import com.adrian.lumina.ui.viewer.ViewerScreen
import java.io.File
import java.time.ZoneId
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Capturas de las pantallas con datos sintéticos (sin dispositivo ni fotos reales). Salida: la carpeta docs/capturas. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-night-xxhdpi")
class ScreenshotsTest {
    @get:Rule val rule = createComposeRule()
    private val zone = ZoneId.of("UTC")
    private val items by lazy { FakePhotos.library(260) }

    @Before fun setUp() { FakePhotos.install(ApplicationProvider.getApplicationContext()) }

    private fun save(name: String) {
        val dir = File(System.getProperty("user.dir"), "../docs/capturas").apply { mkdirs() }
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    private val tabIcons = listOf(Icons.Outlined.PhotoLibrary, Icons.Outlined.PhotoAlbum, Icons.Outlined.Search, Icons.Outlined.Tune)
    private val tabLabels = listOf("Biblioteca", "Álbumes", "Buscar", "Ajustes")

    @androidx.compose.runtime.Composable
    private fun Chrome(selected: Int, content: @androidx.compose.runtime.Composable () -> Unit) {
        Box(Modifier.fillMaxSize()) {
            content()
            LuminaTabs(tabLabels, tabIcons, selected, {}, Modifier.align(Alignment.BottomCenter))
        }
    }

    private fun library(dark: Boolean, level: ZoomLevel, name: String) {
        val state = GalleryUiState.Content(buildTimeline(items, zone, 3f), items.size)
        rule.setContent {
            LuminaTheme(dark = dark) { Chrome(0) { GalleryScreen(state, emptySet(), 4, false, false, GalleryActions(), level = level) } }
        }
        rule.waitForIdle()
        save(name)
    }

    @Test fun libraryDark() = library(true, ZoomLevel.DAYS, "01-biblioteca-oscuro")
    @Test fun libraryLight() = library(false, ZoomLevel.DAYS, "02-biblioteca-claro")
    @Test fun months() = library(true, ZoomLevel.MONTHS, "03-meses")
    @Test fun years() = library(true, ZoomLevel.YEARS, "04-años")

    @Test fun selection() {
        val state = GalleryUiState.Content(buildTimeline(items, zone, 3f), items.size)
        val sel = state.timeline.items.take(5).map { it.id }.toSet()
        rule.setContent { LuminaTheme(dark = true) { GalleryScreen(state, sel, 4, false, false, GalleryActions()) } }
        rule.waitForIdle(); save("05-seleccion")
    }

    @Test fun scrubberDrag() {
        val state = GalleryUiState.Content(buildTimeline(items, zone, 3f), items.size)
        rule.setContent { LuminaTheme(dark = true) { GalleryScreen(state, emptySet(), 4, false, false, GalleryActions(), listState = rememberLazyListState()) } }
        rule.onNodeWithTag("scrubber").performTouchInput { down(Offset(centerX, height * 0.15f)); moveTo(Offset(centerX, height * 0.5f)) }
        rule.waitForIdle(); save("06-scrubber")
        rule.onNodeWithTag("scrubber").performTouchInput { up() }
    }

    @Test fun albums() {
        val albums = listOf(
            Album(1, "Viaje a Lisboa", "", null, true, false, items.take(14).map { it.id }),
            Album(2, "Familia", "", items[30].id, false, false, items.drop(20).take(9).map { it.id }),
            Album(3, "Proyectos", "", null, false, false, items.drop(60).take(5).map { it.id }),
        )
        val ov = albumOverview(items, albums, items.first().dateMillis, false)
        rule.setContent { LuminaTheme(dark = true) { Chrome(1) { AlbumsScreen(ov, false, {}, {}, {}) } } }
        rule.waitForIdle(); save("07-albumes")
    }

    @Test fun search() {
        val res = items.filter { it.isVideo || it.isFavorite }.take(20)
        rule.setContent { LuminaTheme(dark = true) { Chrome(2) { SearchScreen("favoritas", SearchUi.Results(res), {}, {}) } } }
        rule.waitForIdle(); save("08-busqueda")
    }

    @Test fun settingsDark() {
        rule.setContent { LuminaTheme(dark = true) { Chrome(3) { SettingsScreen(AppSettings(), StorageUi(storageSummary(items), 24, 3), "0.1.0", {}, {}) } } }
        rule.waitForIdle(); save("09-ajustes-oscuro")
    }

    @Test fun settingsLight() {
        rule.setContent { LuminaTheme(dark = false) { Chrome(3) { SettingsScreen(AppSettings(), StorageUi(storageSummary(items), 24, 3), "0.1.0", {}, {}) } } }
        rule.waitForIdle(); save("10-ajustes-claro")
    }

    @Test fun onboarding() {
        rule.setContent { LuminaTheme(dark = true) { Onboarding({}, {}) } }
        rule.waitForIdle(); rule.mainClock.advanceTimeBy(1500); rule.waitForIdle(); save("11-bienvenida")
    }

    @Test fun viewer() {
        val list = items.filter { !it.isVideo }.take(12)
        rule.setContent {
            LuminaTheme(dark = true) {
                ViewerScreen(list, list[3].id, {}, {}, {}, {}, {}, onEdit = {}, locationGranted = false, onRequestLocation = {}, loadExif = { null })
            }
        }
        rule.waitForIdle(); rule.mainClock.advanceTimeBy(800); rule.waitForIdle(); save("12-visor")
    }
}
