package com.adrian.lumina

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.geometry.Offset
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.MediaKind
import com.adrian.lumina.ui.theme.LuminaTheme
import com.adrian.lumina.ui.viewer.ViewerScreen
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Reproduce un mp4 real (assets del test) con ExoPlayer: no hay simulación del reproductor. */
@RunWith(AndroidJUnit4::class)
class VideoPlayerTest {
    @get:Rule val rule = createComposeRule()

    private fun clipItem(): MediaItem {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val out = File(ctx.cacheDir, "clip.mp4")
        InstrumentationRegistry.getInstrumentation().context.assets.open("clip.mp4").use { i -> out.outputStream().use { i.copyTo(it) } }
        return MediaItem(1, android.net.Uri.fromFile(out).toString(), "clip.mp4", MediaKind.VIDEO, 0, 320, 240, 4000, out.length(), "", "", false)
    }

    private fun state(tag: String) = rule.onNodeWithTag(tag).fetchSemanticsNode().config
        .getOrNull(SemanticsProperties.StateDescription)

    private fun open() {
        val item = clipItem()
        rule.setContent {
            LuminaTheme {
                ViewerScreen(
                    items = listOf(item), startId = 1, onClose = {}, onPageSettled = {}, onToggleFavorite = {},
                    onShare = {}, onDelete = {}, locationGranted = true, onRequestLocation = {}, loadExif = { null },
                    autoHideChromeMs = Long.MAX_VALUE,
                )
            }
        }
        rule.onNodeWithTag("video-start").performClick()
        rule.waitUntil(10_000) { runCatching { state("video-play-pause") == "playing" }.getOrDefault(false) }
    }

    @Test fun playsAdvancesPausesAndResumes() {
        open()
        rule.waitUntil(8_000) { rule.onAllNodes(hasText("0:01")).fetchSemanticsNodes().isNotEmpty() } // el reloj avanza
        rule.onNodeWithTag("video-play-pause").performClick()
        rule.waitUntil(5_000) { state("video-play-pause") == "paused" }
        rule.onNodeWithTag("video-play-pause").performClick()
        rule.waitUntil(5_000) { state("video-play-pause") == "playing" }
    }

    @Test fun muteTogglesAndDoubleTapSeeksWithFeedback() {
        open()
        val mute = androidx.compose.ui.test.hasContentDescription(rule.activityString(R.string.video_mute))
        val unmute = androidx.compose.ui.test.hasContentDescription(rule.activityString(R.string.video_unmute))
        rule.onNodeWithTag("video-mute").performClick()
        rule.waitUntil(3_000) { rule.onAllNodes(unmute, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("video-mute").performClick()
        rule.waitUntil(3_000) { rule.onAllNodes(mute, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

        rule.onNodeWithTag("video-surface").performTouchInput { doubleClick(Offset(width * 0.85f, height / 2f)) }
        rule.waitUntil(3_000) { rule.onAllNodes(hasText("+10 s")).fetchSemanticsNodes().isNotEmpty() }
        // saltar 10 s en un clip de 4 s lleva al final: aparece «repetir»
        rule.waitUntil(5_000) { rule.onAllNodes(androidx.compose.ui.test.hasTestTag("video-replay")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("video-replay").performClick()
        rule.waitUntil(5_000) { state("video-play-pause") == "playing" }
    }
}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.activityString(id: Int): String =
    ApplicationProvider.getApplicationContext<Context>().getString(id)
