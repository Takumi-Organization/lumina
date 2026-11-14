package com.adrian.lumina.shots

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.adrian.lumina.domain.usecase.buildTimeline
import com.adrian.lumina.ui.gallery.GalleryActions
import com.adrian.lumina.ui.gallery.GalleryScreen
import com.adrian.lumina.ui.gallery.GalleryUiState
import com.adrian.lumina.ui.theme.LuminaTheme
import java.io.File
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Capturas de las pantallas con datos sintéticos (sin dispositivo). Salida: la carpeta docs/capturas */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-night-xxhdpi")
class ScreenshotsTest {
    @get:Rule val rule = createComposeRule()

    private fun save(name: String) {
        val dir = File(System.getProperty("user.dir"), "../docs/capturas").apply { mkdirs() }
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun library() {
        FakePhotos.install(ApplicationProvider.getApplicationContext())
        val items = FakePhotos.library(220)
        val state = GalleryUiState.Content(buildTimeline(items, ZoneId.of("UTC"), 3f), items.size)
        rule.setContent { LuminaTheme(dark = true) { GalleryScreen(state, emptySet(), 4, false, false, GalleryActions()) } }
        rule.waitForIdle()
        save("biblioteca-oscuro")
    }
}
