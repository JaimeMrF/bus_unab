package com.vibra.bus.domain.brand

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Contrato mascot_poses / bus_style y su retrocompatibilidad con perfiles antiguos. */
class BrandPosesTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val palette = """
        "primary":"#2563EB","on_primary":"#FFFFFF","secondary":"#475569","on_secondary":"#FFFFFF",
        "background":"#F8FAFC","surface":"#FFFFFF","on_surface":"#0F172A","accent":"#0EA5E9",
        "success":"#15803D","warning":"#B45309","error":"#B91C1C"
    """.trimIndent()

    private fun profile(extra: String) =
        """{"slug":"a","app_name":"A","colors":{"light":{$palette},"dark":{$palette}} $extra}"""

    private fun decode(raw: String) = json.decodeFromString(BrandConfig.serializer(), raw)

    @Test
    fun oldProfileWithoutPosesOrBusStyleStillParses() {
        val b = decode(profile(""))
        assertNull(b.mascotPoses)
        assertNull(b.busStyle)
        MascotPose.values().forEach { assertNull(b.poseUrl(it), "sin mascota no debe haber URL: $it") }
    }

    @Test
    fun allNullPosesFallBackToSingleMascot() {
        val nulls = MascotPose.values().joinToString(",") { "\"${it.name.lowercase()}\":null" }
        val b = decode(profile(""","mascot_url":"https://cdn.test/m.png","mascot_poses":{$nulls}"""))
        MascotPose.values().forEach { assertEquals("https://cdn.test/m.png", b.poseUrl(it)) }
    }

    @Test
    fun specificPoseWinsAndMissingOnesFallBack() {
        val b = decode(profile(""","mascot_url":"https://cdn.test/m.png","mascot_poses":{"sad":"https://cdn.test/sad.png"}"""))
        assertEquals("https://cdn.test/sad.png", b.poseUrl(MascotPose.Sad))
        assertEquals("https://cdn.test/m.png", b.poseUrl(MascotPose.Greeting))
    }

    @Test
    fun blankUrlsAreTreatedAsMissing() {
        val b = decode(profile(""","mascot_url":"  ","mascot_poses":{"ok":""}"""))
        assertNull(b.poseUrl(MascotPose.Ok))
        assertNull(b.poseUrl(MascotPose.Celebrating))
    }

    @Test
    fun busStyleParsesNullsAndDefaults() {
        val full = decode(profile(""","bus_style":{"body":"#112233","accent":null,"icon":"modern","icon_url":null}"""))
        assertEquals("#112233", full.busStyle?.body)
        assertNull(full.busStyle?.accent)
        assertEquals("modern", full.busStyle?.icon)
        assertNull(full.busStyle?.iconUrl)

        val empty = decode(profile(""","bus_style":{}"""))
        assertEquals("classic", empty.busStyle?.icon)
        assertNull(empty.busStyle?.body)
    }

    @Test
    fun unknownKeysInsidePosesAreIgnored() {
        val b = decode(profile(""","mascot_poses":{"future_pose":"https://x","sad":"https://cdn.test/s.png"}"""))
        assertEquals("https://cdn.test/s.png", b.poseUrl(MascotPose.Sad))
    }

    @Test
    fun poseConfigSurvivesCacheRoundTrip() {
        val b = decode(profile(""","mascot_poses":{"map":"https://cdn.test/map.png"},"bus_style":{"icon":"articulated"}"""))
        val again = decode(json.encodeToString(BrandConfig.serializer(), b))
        assertEquals(b, again)
        assertEquals("https://cdn.test/map.png", again.poseUrl(MascotPose.Map))
    }
}
