package com.adrian.lumina

import com.adrian.lumina.editor.*
import org.junit.Assert.*
import org.junit.Test

class EditPipelineTest {
    private fun rgb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    private fun r(p: Int) = (p shr 16) and 0xFF
    private fun g(p: Int) = (p shr 8) and 0xFF
    private fun b(p: Int) = p and 0xFF
    private fun solid(w: Int, h: Int, c: Int) = PixelImage(w, h, IntArray(w * h) { c })
    private fun lum(p: Int) = 0.2126 * r(p) + 0.7152 * g(p) + 0.0722 * b(p)

    @Test fun identityReturnsSameImage() {
        val img = solid(3, 2, rgb(10, 20, 30))
        assertSame(img, EditPipeline.apply(img, EditState()))
    }

    @Test fun exposureBrightensAndDarkens() {
        val img = solid(2, 2, rgb(100, 100, 100))
        assertTrue(r(EditPipeline.apply(img, EditState(exposure = 0.5f))[0, 0]) > 100)
        assertTrue(r(EditPipeline.apply(img, EditState(exposure = -0.5f))[0, 0]) < 100)
        assertEquals(200, r(EditPipeline.apply(img, EditState(exposure = 1f))[0, 0]))
    }

    @Test fun contrastSpreadsAroundMidGray() {
        val img = PixelImage(2, 1, intArrayOf(rgb(64, 64, 64), rgb(192, 192, 192)))
        val out = EditPipeline.apply(img, EditState(contrast = 0.5f))
        assertTrue(r(out[0, 0]) < 64 && r(out[1, 0]) > 192)
        val flat = EditPipeline.apply(img, EditState(contrast = -1f))
        assertTrue(kotlin.math.abs(r(flat[0, 0]) - 128) <= 1)
    }

    @Test fun fullDesaturationGivesGray() {
        val out = EditPipeline.apply(solid(1, 1, rgb(200, 50, 20)), EditState(saturation = -1f))[0, 0]
        assertTrue(kotlin.math.abs(r(out) - g(out)) <= 1 && kotlin.math.abs(g(out) - b(out)) <= 1)
    }

    @Test fun warmthShiftsRedUpBlueDown() {
        val src = solid(1, 1, rgb(100, 100, 100))
        val warm = EditPipeline.apply(src, EditState(warmth = 1f))[0, 0]
        val cool = EditPipeline.apply(src, EditState(warmth = -1f))[0, 0]
        assertTrue(r(warm) > 100 && b(warm) < 100 && r(cool) < 100 && b(cool) > 100)
    }

    @Test fun shadowsLiftDarkMoreThanBright() {
        val dark = EditPipeline.apply(solid(1, 1, rgb(30, 30, 30)), EditState(shadows = 1f))[0, 0]
        val bright = EditPipeline.apply(solid(1, 1, rgb(230, 230, 230)), EditState(shadows = 1f))[0, 0]
        assertTrue(r(dark) - 30 > 20)
        assertTrue(r(dark) - 30 > (r(bright) - 230) * 5)
    }

    @Test fun highlightsRecoverBrightMoreThanDark() {
        val dark = EditPipeline.apply(solid(1, 1, rgb(30, 30, 30)), EditState(highlights = -1f))[0, 0]
        val bright = EditPipeline.apply(solid(1, 1, rgb(230, 230, 230)), EditState(highlights = -1f))[0, 0]
        assertTrue(230 - r(bright) > 30)
        assertTrue(230 - r(bright) > (30 - r(dark)) * 5)
    }

    @Test fun alphaIsPreserved() {
        val c = (0x80 shl 24) or (100 shl 16) or (100 shl 8) or 100
        val out = EditPipeline.apply(solid(2, 2, c), EditState(exposure = 0.3f, vignette = 0.5f, sharpen = 0.5f))
        assertTrue(out.pixels.all { (it ushr 24) == 0x80 })
    }

    @Test fun sharpenKeepsFlatAndBoostsEdge() {
        val flat = solid(5, 5, rgb(90, 90, 90))
        assertEquals(flat, EditPipeline.apply(flat, EditState(sharpen = 1f)))
        val edge = PixelImage(4, 1, intArrayOf(rgb(50, 50, 50), rgb(50, 50, 50), rgb(200, 200, 200), rgb(200, 200, 200)))
        val out = EditPipeline.apply(edge, EditState(sharpen = 1f))
        assertTrue(r(out[1, 0]) < 50 && r(out[2, 0]) > 200)
    }

    @Test fun vignetteDarkensCornersNotCenter() {
        val out = EditPipeline.apply(solid(21, 21, rgb(200, 200, 200)), EditState(vignette = 1f))
        assertEquals(200, r(out[10, 10]))
        assertTrue(r(out[0, 0]) < 20)
        assertTrue(lum(out[0, 0]) < lum(out[5, 5]))
    }

    @Test fun rotateQuarterTurnMovesPixelsClockwise() {
        // 3×2: [A B C / D E F] → 2×3 girada 90° horario: [D A / E B / F C]
        val img = PixelImage(3, 2, intArrayOf(1, 2, 3, 4, 5, 6))
        val out = EditPipeline.apply(img, EditState(quarterTurns = 1))
        assertEquals(2, out.width); assertEquals(3, out.height)
        assertArrayEquals(intArrayOf(4, 1, 5, 2, 6, 3), out.pixels)
    }

    @Test fun fourTurnsAndTwoFlipsAreIdentity() {
        val img = PixelImage(3, 2, intArrayOf(1, 2, 3, 4, 5, 6))
        assertEquals(img, EditPipeline.orient(EditPipeline.orient(img, 2, false), 2, false))
        assertEquals(img, EditPipeline.orient(EditPipeline.orient(img, 0, true), 0, true))
        assertEquals(img, EditPipeline.orient(img, 4, false))
        assertEquals(EditPipeline.orient(img, 3, false), EditPipeline.orient(img, -1, false))
    }

    @Test fun flipMirrorsRows() {
        val img = PixelImage(3, 2, intArrayOf(1, 2, 3, 4, 5, 6))
        assertArrayEquals(intArrayOf(3, 2, 1, 6, 5, 4), EditPipeline.apply(img, EditState(flipHorizontal = true)).pixels)
    }

    @Test fun cropExtractsRegion() {
        val img = PixelImage(4, 4, IntArray(16) { it })
        val out = EditPipeline.apply(img, EditState(crop = CropRect(0.25f, 0.5f, 0.75f, 1f)))
        assertEquals(2, out.width); assertEquals(2, out.height)
        assertArrayEquals(intArrayOf(9, 10, 13, 14), out.pixels)
    }

    @Test fun cropNeverCollapsesToEmpty() {
        val out = EditPipeline.crop(PixelImage(4, 4, IntArray(16)), CropRect(0.9f, 0.9f, 0.9f, 0.9f))
        assertTrue(out.width >= 1 && out.height >= 1)
    }

    @Test fun centeredCropRatios() {
        val wide = centeredCrop(400, 200, 1f)   // 2:1 → 1:1 recorta lados
        assertEquals(0.25f, wide.left, 1e-4f); assertEquals(0.75f, wide.right, 1e-4f)
        val tall = centeredCrop(200, 400, 1f)
        assertEquals(0.25f, tall.top, 1e-4f); assertEquals(0.75f, tall.bottom, 1e-4f)
        assertTrue(centeredCrop(100, 100, 1f).isFull)
    }

    @Test fun originalIsNeverModified() {
        val img = solid(4, 4, rgb(120, 80, 40))
        val copy = img.pixels.copyOf()
        EditPipeline.apply(img, EditState(exposure = 0.7f, contrast = 0.3f, sharpen = 1f, vignette = 1f, quarterTurns = 1))
        assertArrayEquals(copy, img.pixels)
    }

    @Test fun adjustmentSetClampsAndRoundTrips() {
        assertEquals(1f, Adjustment.EXPOSURE.set(EditState(), 5f).exposure, 0f)
        assertEquals(0f, Adjustment.SHARPEN.set(EditState(), -1f).sharpen, 0f)
        Adjustment.entries.forEach { assertEquals(0.5f, it.get(it.set(EditState(), 0.5f)), 0f) }
    }
}

class UndoStackTest {
    @Test fun undoRedoWalkHistory() {
        var h = UndoStack(0).push(1).push(2)
        assertEquals(2, h.current); assertTrue(h.canUndo); assertFalse(h.canRedo)
        h = h.undo(); assertEquals(1, h.current); assertTrue(h.canRedo)
        h = h.undo().undo(); assertEquals(0, h.current); assertFalse(h.canUndo)
        h = h.redo().redo(); assertEquals(2, h.current); assertFalse(h.canRedo)
    }

    @Test fun pushClearsRedo() {
        val h = UndoStack(0).push(1).push(2).undo().push(9)
        assertEquals(9, h.current); assertFalse(h.canRedo); assertEquals(1, h.undo().current)
    }

    @Test fun samePushAddsNothingAndLimitDropsOldest() {
        val same = UndoStack(0).push(0)
        assertFalse(same.canUndo)
        var h = UndoStack(0, limit = 3)
        for (i in 1..10) h = h.push(i)
        var n = 0
        while (h.canUndo) { h = h.undo(); n++ }
        assertEquals(3, n); assertEquals(7, h.current)
    }

    @Test fun undoOnEmptyIsNoOp() {
        val h = UndoStack("a")
        assertSame(h, h.undo()); assertSame(h, h.redo())
    }
}

class CropGeometryTest {
    private val full = CropRect()
    private fun near(a: Float, b: Float) = kotlin.math.abs(a - b) < 1e-5f

    @Test fun rotateFourTimesIsIdentity() {
        val r = CropRect(0.1f, 0.2f, 0.6f, 0.9f)
        var x = r; repeat(4) { x = x.rotatedClockwise() }
        assertTrue(near(x.left, r.left) && near(x.top, r.top) && near(x.right, r.right) && near(x.bottom, r.bottom))
    }

    @Test fun rotateKeepsTheSameContent() {
        // 4×2 con las columnas 0..1 recortadas; tras girar 90° horario la imagen es 2×4 y el recorte son sus filas 0..1.
        val img = PixelImage(4, 2, IntArray(8) { it })
        val crop = CropRect(0f, 0f, 0.5f, 1f)
        val before = EditPipeline.apply(img, EditState(crop = crop)).pixels.toSet()
        val after = EditPipeline.apply(img, EditState(quarterTurns = 1, crop = crop.rotatedClockwise())).pixels.toSet()
        assertEquals(before, after)
    }

    @Test fun mirrorKeepsTheSameContent() {
        val img = PixelImage(4, 2, IntArray(8) { it })
        val crop = CropRect(0f, 0f, 0.5f, 1f)
        val before = EditPipeline.apply(img, EditState(crop = crop)).pixels.toSet()
        val after = EditPipeline.apply(img, EditState(flipHorizontal = true, crop = crop.mirrored())).pixels.toSet()
        assertEquals(before, after)
    }

    @Test fun cropIsAppliedAfterRotation() {
        val img = PixelImage(4, 2, IntArray(8) { it })
        val out = EditPipeline.apply(img, EditState(quarterTurns = 1, crop = CropRect(0f, 0f, 1f, 0.5f)))
        assertEquals(2, out.width); assertEquals(2, out.height)
    }

    @Test fun hitTestFindsCornersEdgesBodyAndOutside() {
        val r = CropRect(0.2f, 0.2f, 0.8f, 0.8f)
        assertEquals(CropHandle.TOP_LEFT, hitHandle(r, 0.21f, 0.19f))
        assertEquals(CropHandle.BOTTOM_RIGHT, hitHandle(r, 0.8f, 0.8f))
        assertEquals(CropHandle.TOP, hitHandle(r, 0.5f, 0.2f))
        assertEquals(CropHandle.LEFT, hitHandle(r, 0.2f, 0.5f))
        assertEquals(CropHandle.MOVE, hitHandle(r, 0.5f, 0.5f))
        assertNull(hitHandle(r, 0.05f, 0.05f))
    }

    @Test fun dragMovesEdgesAndStaysInBounds() {
        val r = CropRect(0.2f, 0.2f, 0.8f, 0.8f)
        val a = dragCrop(r, CropHandle.TOP_LEFT, 0.1f, 0.1f)
        assertTrue(near(a.left, 0.3f) && near(a.top, 0.3f) && near(a.right, 0.8f))
        val b = dragCrop(r, CropHandle.RIGHT, 0.5f, 0f)
        assertEquals(1f, b.right, 0f)
        val m = dragCrop(r, CropHandle.MOVE, 0.9f, -0.9f)
        assertTrue(near(m.right, 1f) && near(m.top, 0f) && near(m.width, 0.6f) && near(m.height, 0.6f))
    }

    @Test fun dragNeverCollapsesBelowMinimum() {
        val r = CropRect(0.2f, 0.2f, 0.8f, 0.8f)
        val a = dragCrop(r, CropHandle.LEFT, 5f, 0f)
        assertEquals(MIN_CROP, a.width, 1e-5f)
        val b = dragCrop(r, CropHandle.BOTTOM, 0f, -5f)
        assertEquals(MIN_CROP, b.height, 1e-5f)
        assertEquals(0f, dragCrop(r, CropHandle.TOP_LEFT, -5f, -5f).left, 0f)
    }

    @Test fun fullRectIsFull() { assertTrue(full.isFull); assertFalse(CropRect(0.1f, 0f, 1f, 1f).isFull) }
}
