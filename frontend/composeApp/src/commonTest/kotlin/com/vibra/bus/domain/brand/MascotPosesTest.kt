package com.vibra.bus.domain.brand

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MascotPosesTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun brand(extra: String) = json.decodeFromString(
        BrandConfig.serializer(),
        """{"slug":"x","app_name":"X","colors":{"light":${pal()},"dark":${pal()}}$extra}""",
    )

    private fun pal() =
        """{"primary":"#112233","on_primary":"#FFFFFF","secondary":"#445566","on_secondary":"#FFFFFF","background":"#FFFFFF","surface":"#FFFFFF","on_surface":"#000000","accent":"#778899","success":"#00AA00","warning":"#AA7700","error":"#AA0000"}"""

    @Test fun oldPayloadWithoutPosesStillParses() {
        val b = brand("")
        assertNull(b.mascotPoses)
        assertNull(b.poseUrl(MascotPose.Sad))
    }

    @Test fun poseFallsBackToSingleMascot() {
        val b = brand(""","mascot_url":"https://a/m.webp","mascot_poses":{"sad":"https://a/s.webp","ok":null}""")
        assertEquals("https://a/s.webp", b.poseUrl(MascotPose.Sad))
        assertEquals("https://a/m.webp", b.poseUrl(MascotPose.Ok))
        assertEquals("https://a/m.webp", b.poseUrl(MascotPose.Curious))
    }

    @Test fun busStyleTolerantToNulls() {
        val b = brand(""","bus_style":{"body":null,"accent":"#FCBB01","icon":null,"icon_url":null}""")
        assertNull(b.busStyle?.body)
        assertEquals("#FCBB01", b.busStyle?.accent)
    }
}
