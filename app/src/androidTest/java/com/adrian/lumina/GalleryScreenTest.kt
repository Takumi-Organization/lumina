package com.adrian.lumina

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.MediaKind
import com.adrian.lumina.domain.usecase.buildTimeline
import com.adrian.lumina.domain.usecase.toggleGroup
import com.adrian.lumina.ui.gallery.GalleryActions
import com.adrian.lumina.ui.gallery.GalleryScreen
import com.adrian.lumina.ui.gallery.GalleryUiState
import com.adrian.lumina.ui.theme.LuminaTheme
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GalleryScreenTest {
    @get:Rule val rule = createComposeRule()

    private val zone = ZoneId.systemDefault()
    // 600 elementos repartidos en ~200 días de 3 años: lo bastante largo para necesitar scroll.
    private val items = (1L..600L).map { i ->
        val day = LocalDate.of(2024, 12, 28).minusDays(i / 3 * 5)
        MediaItem(i, "content://media/none/$i", "IMG_$i.jpg", MediaKind.IMAGE,
            day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli() - i, 10, 10, 0, 1, "Camera", "DCIM/", false)
    }
    private val state = GalleryUiState.Content(buildTimeline(items, zone), items.size)

    @Test fun longPressSelectsTapTogglesAndTapWithoutSelectionOpens() {
        var selection by mutableStateOf(emptySet<Long>())
        val opened = mutableListOf<Long>()
        rule.setContent {
            LuminaTheme {
                GalleryScreen(
                    state, selection, 4, false, false,
                    GalleryActions(
                        onToggle = { selection = if (it in selection) selection - it else selection + it },
                        onToggleDay = { selection = selection.toggleGroup(it) },
                        onClearSelection = { selection = emptySet() },
                        onOpen = { opened += it },
                    ),
                )
            }
        }
        val first = state.timeline.items.first().id
        val second = items.first { it.id != first }.id

        rule.onNodeWithTag("cell-$first").performClick()
        assertEquals(listOf(first), opened)            // sin selección, abrir
        assertTrue(selection.isEmpty())

        rule.onNodeWithTag("cell-$first").performTouchInput { longClick() }
        rule.waitForIdle()
        assertEquals(setOf(first), selection)
        rule.onNodeWithTag("cell-$first").assertIsSelected()
        rule.onNodeWithTag("selection-count").assertIsDisplayed()

        rule.onNodeWithTag("cell-$second").performClick()
        assertEquals(setOf(first, second), selection)
        rule.onNodeWithTag("cell-$second").assertIsSelected()

        rule.onNodeWithTag("cell-$first").performClick()
        rule.onNodeWithTag("cell-$first").assertIsNotSelected()
        assertEquals(1, opened.size)                   // con selección activa, tocar no abre

        rule.onNodeWithTag("selection-close").performClick()
        assertTrue(selection.isEmpty())
    }

    @Test fun draggingScrubberJumpsFarAndShowsMonthBubble() {
        var gridState: androidx.compose.foundation.lazy.LazyListState? = null
        rule.setContent {
            LuminaTheme {
                val gs = androidx.compose.foundation.lazy.rememberLazyListState().also { gridState = it }
                GalleryScreen(state, emptySet(), 4, false, false, GalleryActions(), listState = gs)
            }
        }
        rule.runOnIdle { assertEquals(0, gridState!!.firstVisibleItemIndex) }
        rule.onNodeWithTag("scrubber").performTouchInput {
            down(androidx.compose.ui.geometry.Offset(centerX, 20f))
            moveTo(androidx.compose.ui.geometry.Offset(centerX, height * 0.9f))
        }
        rule.waitForIdle()
        rule.onNodeWithTag("scrubber-bubble").assertIsDisplayed()
        val idx = rule.runOnIdle { gridState!!.firstVisibleItemIndex }
        val total = state.timeline.entries.size
        assertTrue("debería estar cerca del final: $idx de $total", idx > total * 0.6)
        rule.onNodeWithTag("scrubber").performTouchInput { up() }
    }

    @Test fun pinchChangesColumnsAndSingleFingerScrollDoesNot() {
        val requested = mutableListOf<Int>()
        rule.setContent {
            LuminaTheme { GalleryScreen(state, emptySet(), 4, false, false, GalleryActions(onColumns = { requested += it })) }
        }
        rule.onNodeWithTag("cell-${items.first().id}").performTouchInput { swipeDown() }
        assertTrue("un dedo no debe cambiar columnas: $requested", requested.isEmpty())
        rule.onRoot().performTouchInput { pinch(Offset(centerX - 60, centerY), Offset(centerX - 400, centerY), Offset(centerX + 60, centerY), Offset(centerX + 400, centerY)) }
        rule.waitForIdle()
        assertTrue("separar dedos = menos columnas: $requested", requested.isNotEmpty() && requested.all { it < 4 })
        requested.clear()
        rule.onRoot().performTouchInput { pinch(Offset(centerX - 400, centerY), Offset(centerX - 60, centerY), Offset(centerX + 400, centerY), Offset(centerX + 60, centerY)) }
        rule.waitForIdle()
        assertTrue("juntar dedos = más columnas: $requested", requested.isNotEmpty() && requested.all { it > 4 })
    }
}
