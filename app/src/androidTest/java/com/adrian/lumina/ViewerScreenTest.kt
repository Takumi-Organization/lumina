package com.adrian.lumina

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adrian.lumina.domain.model.ExifInfo
import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.MediaKind
import com.adrian.lumina.ui.theme.LuminaTheme
import com.adrian.lumina.ui.viewer.ViewerScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTextCount(text: String) =
    onAllNodes(androidx.compose.ui.test.hasText(text)).fetchSemanticsNodes().size

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.activity() =
    androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()

@RunWith(AndroidJUnit4::class)
class ViewerScreenTest {
    @get:Rule val rule = createComposeRule()

    private fun item(id: Long, video: Boolean = false) = MediaItem(
        id, "content://media/none/$id", "IMG_$id.jpg", if (video) MediaKind.VIDEO else MediaKind.IMAGE,
        1_700_000_000_000 + id, 4000, 3000, if (video) 8000 else 0, 2_900_000, "Camera", "DCIM/Camera/", false,
    )
    private val items = listOf(item(1), item(2), item(3))
    private val exif = ExifInfo("Lumina Labs", "Prueba 1", 1.8, 0.004, 200, 6.9, 37.8882, -4.7794)

    private val settled = mutableListOf<Long>()
    private val favorited = mutableListOf<Long>()
    private val shared = mutableListOf<Long>()
    private val deleted = mutableListOf<Long>()
    private var closed = 0
    private var locationRequests = 0

    private fun show(list: List<MediaItem> = items, locationGranted: Boolean = true, start: Long = 1) {
        rule.setContent {
            LuminaTheme {
                ViewerScreen(
                    items = list, startId = start, onClose = { closed++ }, onPageSettled = { settled += it },
                    onToggleFavorite = { favorited += it.id }, onShare = { shared += it.id }, onDelete = { deleted += it.id },
                    locationGranted = locationGranted, onRequestLocation = { locationRequests++ },
                    loadExif = { if (locationGranted) exif else exif.copy(latitude = null, longitude = null) },
                )
            }
        }
    }

    @Test fun swipeChangesPageAndReportsIt() {
        show()
        rule.onNodeWithTag("viewer-counter").assertTextEquals("1 / 3")
        rule.onNodeWithTag("viewer-pager").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.onNodeWithTag("viewer-counter").assertTextEquals("2 / 3")
        assertTrue("debe avisar de la página asentada: $settled", settled.last() == 2L)
    }

    @Test fun startsOnRequestedItem() {
        show(start = 3)
        rule.onNodeWithTag("viewer-counter").assertTextEquals("3 / 3")
    }

    @Test fun zoomedImageBlocksPagingUntilZoomOut() {
        show()
        rule.onNodeWithTag("viewer-pager").performTouchInput { doubleClick() }
        rule.waitForIdle()
        rule.onNodeWithTag("viewer-pager").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.onNodeWithTag("viewer-counter").assertTextEquals("1 / 3") // con zoom no pasa de página
        rule.onNodeWithTag("viewer-pager").performTouchInput { doubleClick() }
        rule.waitForIdle()
        rule.onNodeWithTag("viewer-pager").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.onNodeWithTag("viewer-counter").assertTextEquals("2 / 3")
    }

    @Test fun actionsCallBackWithCurrentItem() {
        show(start = 2)
        rule.onNodeWithTag("viewer-favorite").performClick()
        rule.onNodeWithTag("viewer-share").performClick()
        rule.onNodeWithTag("viewer-delete").performClick()
        rule.onNodeWithTag("viewer-close").performClick()
        assertEquals(listOf(2L), favorited); assertEquals(listOf(2L), shared); assertEquals(listOf(2L), deleted)
        assertEquals(1, closed)
    }

    @Test fun infoSheetShowsMetadataAndLocation() {
        show()
        rule.onNodeWithTag("viewer-info").performClick()
        rule.waitUntil(3000) { rule.onAllNodesWithTextCount("37.8882° N, 4.7794° W") > 0 }
        rule.onNodeWithTag("info-sheet").assertIsDisplayed()
        rule.onNodeWithText("IMG_1.jpg").assertExists()
        rule.onNodeWithText("Lumina Labs Prueba 1").assertExists()
        rule.onNodeWithText("f/1.8  ·  1/250 s  ·  ISO 200  ·  6.9 mm").assertExists()
    }

    @Test fun infoSheetAsksForLocationPermissionWhenNotGranted() {
        show(locationGranted = false)
        rule.onNodeWithTag("viewer-info").performClick()
        val allow = rule.activity().getString(R.string.info_location_allow)
        rule.waitUntil(3000) { rule.onAllNodesWithTextCount(allow) > 0 }
        rule.onNodeWithText(allow).performClick()
        assertEquals(1, locationRequests)
    }

    @Test fun controlsHideByThemselvesAndTapBringsThemBack() {
        rule.mainClock.autoAdvance = false
        show()
        rule.onNodeWithTag("viewer-counter").assertExists()
        rule.mainClock.advanceTimeBy(5000)
        rule.onNodeWithTag("viewer-counter").assertDoesNotExist()
        rule.onNodeWithTag("viewer-pager").performTouchInput { click() }
        rule.mainClock.advanceTimeBy(600) // el toque espera por si es un doble toque
        rule.onNodeWithTag("viewer-counter").assertExists()
    }

    @Test fun landscapeConfigurationComposesAndKeepsControls() {
        rule.setContent {
            val landscape = android.content.res.Configuration(androidx.compose.ui.platform.LocalConfiguration.current)
                .apply { orientation = android.content.res.Configuration.ORIENTATION_LANDSCAPE }
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalConfiguration provides landscape) {
                LuminaTheme {
                    ViewerScreen(
                        items = items, startId = 1, onClose = {}, onPageSettled = {}, onToggleFavorite = {},
                        onShare = {}, onDelete = {}, locationGranted = true, onRequestLocation = {}, loadExif = { null },
                        autoHideChromeMs = Long.MAX_VALUE,
                    )
                }
            }
        }
        rule.onNodeWithTag("viewer-counter").assertTextEquals("1 / 3")
        rule.onNodeWithTag("viewer-delete").assertIsDisplayed()
    }
}
