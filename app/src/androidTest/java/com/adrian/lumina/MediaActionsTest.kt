package com.adrian.lumina

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adrian.lumina.data.media.MediaActions
import com.adrian.lumina.domain.model.MediaItem
import com.adrian.lumina.domain.model.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaActionsTest {
    private val actions = MediaActions(ApplicationProvider.getApplicationContext())
    private fun item(id: Long, video: Boolean = false) = MediaItem(
        id, "content://media/external/images/media/$id", "$id", if (video) MediaKind.VIDEO else MediaKind.IMAGE,
        0, 1, 1, 0, 1, "", "", false,
    )

    @Suppress("DEPRECATION")
    private fun inner(i: Intent) = i.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!

    @Test fun singleImageUsesSendWithGrant() {
        val send = inner(actions.shareChooser(listOf(item(1))))
        assertEquals(Intent.ACTION_SEND, send.action); assertEquals("image/*", send.type)
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals(1, send.clipData!!.itemCount)
    }

    @Test fun mixedSelectionUsesSendMultipleAndWildcardType() {
        val send = inner(actions.shareChooser(listOf(item(1), item(2, video = true), item(3))))
        assertEquals(Intent.ACTION_SEND_MULTIPLE, send.action); assertEquals("*/*", send.type)
        assertEquals(3, send.clipData!!.itemCount)
    }

    @Test fun videosOnlyGetVideoType() {
        assertEquals("video/*", inner(actions.shareChooser(listOf(item(1, true), item(2, true)))).type)
    }
}
