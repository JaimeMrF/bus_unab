package com.vibra.bus.domain.brand

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Parseo del contrato GET /api/v1/branding/{slug}, tema neutro y contraste AA. */
class BrandConfigTest {

    // Misma configuración que BrandRepository.
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val paletteLight = """
        "primary":"#2563EB","on_primary":"#FFFFFF","secondary":"#475569","on_secondary":"#FFFFFF",
        "background":"#F8FAFC","surface":"#FFFFFF","on_surface":"#0F172A","accent":"#0EA5E9",
        "success":"#15803D","warning":"#B45309","error":"#B91C1C"
    """.trimIndent()

    private val paletteDark = """
        "primary":"#60A5FA","on_primary":"#0B1220","secondary":"#94A3B8","on_secondary":"#0B1220",
        "background":"#0B1220","surface":"#111827","on_surface":"#E5E7EB","accent":"#38BDF8",
        "success":"#4ADE80","warning":"#FBBF24","error":"#F87171"
    """.trimIndent()

    private fun full(extra: String = "") = """
        {
          "success": true, "message": "OK",
          "slug":"acme","app_name":"Acme Go","tagline":"Muévete fácil","support_email":"ayuda@acme.test",
          "logo_url":"https://cdn.test/l.png","logo_dark_url":null,"icon_url":"https://cdn.test/i.png","mascot_url":null,
          "colors":{"light":{$paletteLight},"dark":{$paletteDark}},
          "font_family":"inter","corner_radius":"lg",
          "features":{"qr_payments":false,"wallet":true,"driver_mode":false},
          "version":7 $extra
        }
    """.trimIndent()

    private fun decode(raw: String) = json.decodeFromString(BrandConfig.serializer(), raw)

    // --- Parseo ---------------------------------------------------------

    @Test
    fun parsesFullContract() {
        val b = decode(full())
        assertEquals("acme", b.slug)
        assertEquals("Acme Go", b.appName)
        assertEquals("Muévete fácil", b.tagline)
        assertEquals("ayuda@acme.test", b.supportEmail)
        assertEquals("https://cdn.test/l.png", b.logoUrl)
        assertNull(b.logoDarkUrl)
        assertNull(b.mascotUrl)
        assertEquals("#2563EB", b.colors.light.primary)
        assertEquals("#0B1220", b.colors.dark.onPrimary)
        assertEquals("inter", b.fontFamily)
        assertEquals("lg", b.cornerRadius)
        assertFalse(b.features.qrPayments)
        assertTrue(b.features.wallet)
        assertFalse(b.features.driverMode)
        assertEquals(7, b.version)
    }

    @Test
    fun ignoresUnknownKeysForForwardCompatibility() {
        val b = decode(full(""","nueva_clave":{"x":1}"""))
        assertEquals("acme", b.slug)
    }

    @Test
    fun missingOptionalFieldsGetSafeDefaults() {
        val raw = """
            {"slug":"acme","app_name":"Acme",
             "colors":{"light":{$paletteLight},"dark":{$paletteDark}}}
        """.trimIndent()
        val b = decode(raw)
        assertNull(b.tagline)
        assertNull(b.logoUrl)
        assertEquals("md", b.cornerRadius)
        assertTrue(b.features.qrPayments && b.features.wallet && b.features.driverMode)
        assertEquals(0, b.version)
    }

    @Test
    fun logoForDarkFallsBackToLightLogo() {
        val b = decode(full())
        assertEquals("https://cdn.test/l.png", b.logoFor(dark = true))
        assertEquals("https://cdn.test/l.png", b.logoFor(dark = false))
        assertEquals("https://cdn.test/d.png", b.copy(logoDarkUrl = "https://cdn.test/d.png").logoFor(dark = true))
    }

    @Test
    fun roundTripsThroughSerialization() {
        val original = decode(full())
        val again = decode(json.encodeToString(BrandConfig.serializer(), original))
        assertEquals(original, again)
    }

    /**
     * Un JSON sin colores no es decodificable: el repositorio debe tratarlo como
     * "sin marca" y caer al tema neutro (ver BrandRepository.loadCached → runCatching).
     */
    @Test
    fun missingColorsFailsToDecodeSoCallerCanFallBackToNeutral() {
        assertFailsWith<SerializationException> {
            decode("""{"slug":"acme","app_name":"Acme"}""")
        }
    }

    @Test
    fun incompletePaletteFailsToDecode() {
        val raw = """{"slug":"a","app_name":"A","colors":{"light":{"primary":"#2563EB"},"dark":{$paletteDark}}}"""
        assertFailsWith<SerializationException> { decode(raw) }
    }

    @Test
    fun garbageJsonFailsToDecode() {
        assertFailsWith<SerializationException> { decode("<html>502 Bad Gateway</html>") }
    }

    // --- Hex ------------------------------------------------------------

    @Test
    fun parseHexColorValid() {
        assertEquals(Color(0xFF2563EB), parseHexColor("#2563EB", Color.Magenta))
        assertEquals(Color(0xFF2563EB), parseHexColor("#2563eb", Color.Magenta))
        assertEquals(Color(0xFF000000), parseHexColor("#000000", Color.Magenta))
    }

    @Test
    fun parseHexColorInvalidReturnsFallback() {
        val fb = Color.Magenta
        listOf("", "#", "#FFF", "2563EB1", "#GGGGGG", "red", "#12345", "#2563EBFF", "rgb(1,2,3)", "#+12345").forEach {
            assertEquals(fb, parseHexColor(it, fb), "hex='$it'")
        }
    }

    @Test
    fun parseHexColorWithoutHashStillParsesSixDigits() {
        // La API siempre envía '#'; el cliente tolera su ausencia.
        assertEquals(Color(0xFF2563EB), parseHexColor("2563EB", Color.Magenta))
    }

    // --- Tema neutro ----------------------------------------------------

    @Test
    fun neutralThemeHasNoBrandingAndAllValidHex() {
        val n = BrandConfig.Neutral
        assertEquals("", n.slug)
        assertNull(n.logoUrl)
        assertNull(n.mascotUrl)
        assertNull(n.tagline)
        val banned = listOf("unab", "bucaratransit", "vibrabus", "leopard", "vibra")
        banned.forEach { assertFalse(n.appName.lowercase().contains(it), "app_name neutro contiene marca: $it") }
        allColors(n).forEach { (where, hex) ->
            assertTrue(isValidHex(hex), "$where inválido: $hex")
            assertNotNull(parseHexColor(hex, Color.Unspecified).takeIf { it != Color.Unspecified }, where)
        }
    }

    // --- Contraste WCAG AA ---------------------------------------------

    @Test
    fun contrastHelperKnownValues() {
        assertEquals(21.0, contrast("#000000", "#FFFFFF"), 0.01)
        assertEquals(1.0, contrast("#777777", "#777777"), 0.01)
        assertTrue(contrast("#767676", "#FFFFFF") >= 4.5)
        assertTrue(contrast("#777777", "#FFFFFF") < 4.5)
    }

    @Test
    fun neutralThemeMeetsAaInBothModes() = assertAa(BrandConfig.Neutral)

    @Test
    fun demoThemeMeetsAaInBothModes() = assertAa(BrandConfig.Demo)

    @Test
    fun parsedApiPaletteMeetsAaInBothModes() = assertAa(decode(full()))

    private fun assertAa(b: BrandConfig) {
        listOf("light" to b.colors.light, "dark" to b.colors.dark).forEach { (mode, p) ->
            listOf(
                "on_primary/primary" to (p.onPrimary to p.primary),
                "on_secondary/secondary" to (p.onSecondary to p.secondary),
                "on_surface/surface" to (p.onSurface to p.surface),
                "on_surface/background" to (p.onSurface to p.background),
            ).forEach { (name, pair) ->
                val ratio = contrast(pair.first, pair.second)
                assertTrue(ratio >= 4.5, "$mode $name = ${(ratio * 100).toInt() / 100.0} < 4.5 (slug='${b.slug}')")
            }
        }
    }

    // --- Utilidades -----------------------------------------------------

    private fun allColors(b: BrandConfig): List<Pair<String, String>> =
        listOf("light" to b.colors.light, "dark" to b.colors.dark).flatMap { (m, p) ->
            listOf(
                "$m.primary" to p.primary, "$m.on_primary" to p.onPrimary,
                "$m.secondary" to p.secondary, "$m.on_secondary" to p.onSecondary,
                "$m.background" to p.background, "$m.surface" to p.surface,
                "$m.on_surface" to p.onSurface, "$m.accent" to p.accent,
                "$m.success" to p.success, "$m.warning" to p.warning, "$m.error" to p.error,
            )
        }

    private fun isValidHex(s: String) = Regex("^#[0-9A-Fa-f]{6}$").matches(s)

    private fun luminance(hex: String): Double {
        val v = hex.removePrefix("#").toInt(16)
        fun ch(shift: Int): Double {
            val c = ((v shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * ch(16) + 0.7152 * ch(8) + 0.0722 * ch(0)
    }

    private fun contrast(a: String, b: String): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }
}
