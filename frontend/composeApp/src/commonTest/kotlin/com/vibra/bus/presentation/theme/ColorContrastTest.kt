package com.vibra.bus.presentation.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Utilidades WCAG que garantizan AA aunque la marca del tenant no lo cumpla. */
class ColorContrastTest {

    private val white = Color.White
    private val black = Color.Black
    private val yellow = Color(0xFFFFFF00)
    private val blue = Color(0xFF2563EB)

    @Test
    fun contrastRatioExtremesAndSymmetry() {
        assertEquals(21f, contrastRatio(black, white), 0.05f)
        assertEquals(1f, contrastRatio(blue, blue), 0.001f)
        assertEquals(contrastRatio(blue, white), contrastRatio(white, blue), 0.001f)
    }

    @Test
    fun readableOnPicksBestOfBlackOrWhite() {
        assertEquals(white, readableOn(black))
        assertEquals(black, readableOn(white))
        assertEquals(black, readableOn(yellow))
        assertEquals(white, readableOn(Color(0xFF0B1220)))
    }

    @Test
    fun ensureContrastKeepsColorThatAlreadyPasses() {
        assertEquals(white, ensureContrast(white, blue))
        assertEquals(black, ensureContrast(black, white))
    }

    @Test
    fun ensureContrastFixesWhiteOnYellow() {
        val fixed = ensureContrast(white, yellow)
        assertTrue(contrastRatio(fixed, yellow) >= 4.5f, "ratio=${contrastRatio(fixed, yellow)}")
    }

    @Test
    fun ensureContrastAlwaysReachesAaOnManyBackgrounds() {
        val backgrounds = listOf(
            yellow, Color(0xFF777777), Color(0xFF808080), Color(0xFFFF8800), Color(0xFF00FF00),
            Color(0xFF00FFFF), Color(0xFFFF0000), Color(0xFF123456), Color(0xFFABCDEF),
        )
        backgrounds.forEach { bg ->
            listOf(white, black, blue, yellow, bg).forEach { fg ->
                val out = ensureContrast(fg, bg)
                assertTrue(contrastRatio(out, bg) >= 4.5f, "fg=$fg bg=$bg -> ${contrastRatio(out, bg)}")
            }
        }
    }

    @Test
    fun ensureContrastHonoursCustomMinimum() {
        val out = ensureContrast(Color(0xFF999999), white, min = 7f)
        assertTrue(contrastRatio(out, white) >= 7f)
    }
}
