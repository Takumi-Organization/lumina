package com.adrian.lumina

import com.adrian.lumina.domain.usecase.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteTest {
    private fun rgb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    @Test fun hslRoundTripKeepsColor() {
        for (c in listOf(rgb(200, 40, 30), rgb(20, 160, 90), rgb(40, 60, 220), rgb(128, 128, 128), rgb(250, 250, 10))) {
            val back = hslToRgb(rgbToHsl(c))
            for (shift in intArrayOf(16, 8, 0)) assertTrue(Math.abs(((c shr shift) and 0xFF) - ((back shr shift) and 0xFF)) <= 2)
        }
    }

    @Test fun solidColorIsItsOwnDominant() {
        val d = dominantColor(IntArray(50) { rgb(30, 120, 220) })
        assertTrue(Math.abs(((d shr 16) and 0xFF) - 30) <= 1 && Math.abs((d and 0xFF) - 220) <= 1)
    }

    @Test fun saturatedColorBeatsGreyMajority() {
        val px = IntArray(100) { if (it < 70) rgb(120, 120, 120) else rgb(20, 90, 230) }
        val h = rgbToHsl(dominantColor(px))
        assertTrue("debe tirar al azul: $h", h.h in 200f..240f && h.s > 0.2f)
    }

    @Test fun emptyAndGreyAreSafe() {
        assertEquals(0xFF3A3B44.toInt(), dominantColor(IntArray(0)))
        assertEquals(0f, rgbToHsl(dominantColor(IntArray(9) { rgb(90, 90, 90) })).s, 0.02f)
    }

    @Test fun backgroundIsDarkAndGlowIsLighterWithSameHue() {
        val src = rgb(230, 90, 40)
        val bg = rgbToHsl(backgroundTint(src)); val glow = rgbToHsl(glowTint(src)); val orig = rgbToHsl(src)
        assertTrue(bg.l <= 0.1f && glow.l > 0.3f)
        assertEquals(orig.h, bg.h, 6f); assertEquals(orig.h, glow.h, 6f)
    }
}
