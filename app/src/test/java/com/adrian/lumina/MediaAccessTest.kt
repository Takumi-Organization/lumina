package com.adrian.lumina

import com.adrian.lumina.domain.model.MediaAccess
import com.adrian.lumina.domain.model.MediaKind
import com.adrian.lumina.domain.model.kindFromMime
import com.adrian.lumina.domain.model.mediaAccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaAccessTest {
    private fun a(sdk: Int, img: Boolean = false, vid: Boolean = false, sel: Boolean = false, legacy: Boolean = false) = mediaAccess(sdk, img, vid, sel, legacy)

    @Test fun android14FullNeedsPhotosAndVideos() = assertEquals(MediaAccess.FULL, a(34, img = true, vid = true))

    @Test fun android14SelectedPhotosOnlyIsPartialNotFull() = assertEquals(MediaAccess.PARTIAL, a(34, sel = true))

    @Test fun android14PhotosWithoutVideosIsPartial() = assertEquals(MediaAccess.PARTIAL, a(34, img = true, sel = true))

    @Test fun android14VideosOnlyIsPartial() = assertEquals(MediaAccess.PARTIAL, a(35, vid = true))

    @Test fun nothingGrantedIsNone() { assertEquals(MediaAccess.NONE, a(34)); assertEquals(MediaAccess.NONE, a(33)); assertEquals(MediaAccess.NONE, a(30)) }

    @Test fun android13UsesTwoPermissionsAndIgnoresUserSelected() {
        assertEquals(MediaAccess.FULL, a(33, img = true, vid = true))
        assertEquals(MediaAccess.PARTIAL, a(33, img = true))
        assertEquals(MediaAccess.NONE, a(33, sel = true)) // el permiso «seleccionadas» no existe antes de la API 34
    }

    @Test fun legacyStorageIsAllOrNothing() {
        assertEquals(MediaAccess.FULL, a(30, legacy = true))
        assertEquals(MediaAccess.NONE, a(32, img = true, vid = true))
    }

    @Test fun mimeClassification() {
        assertEquals(MediaKind.IMAGE, kindFromMime("image/heic"))
        assertEquals(MediaKind.IMAGE, kindFromMime("IMAGE/WEBP"))
        assertEquals(MediaKind.VIDEO, kindFromMime("video/x-matroska"))
        assertNull(kindFromMime("audio/mpeg")); assertNull(kindFromMime("application/octet-stream")); assertNull(kindFromMime(null)); assertNull(kindFromMime(""))
    }
}
