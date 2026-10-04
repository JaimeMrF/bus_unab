package com.vibra.bus.presentation.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BusMotionTest {

    @Test
    fun headingTakesShortestPathAcrossZero() {
        assertEquals(0f, lerpHeading(350f, 10f, 0.5f), 0.001f)
        assertEquals(0f, lerpHeading(10f, 350f, 0.5f), 0.001f)
    }

    @Test
    fun headingEndpointsAndNormalisation() {
        assertEquals(90f, lerpHeading(90f, 270f, 0f), 0.001f)
        val end = lerpHeading(350f, 10f, 1f)
        assertEquals(10f, end, 0.001f)
        for (t in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
            val h = lerpHeading(300f, 20f, t)
            assertTrue(h >= 0f && h < 360f, "rumbo fuera de [0,360): $h")
        }
    }

    @Test
    fun poseInterpolatesLinearly() {
        val mid = lerpPose(BusPose(0.0, 0.0, 0f), BusPose(10.0, -20.0, 90f), 0.5f)
        assertEquals(5.0, mid.latitude, 1e-9)
        assertEquals(-10.0, mid.longitude, 1e-9)
        assertEquals(45f, mid.heading, 0.001f)
    }

    @Test
    fun newBusAppearsAtDestinationAndRemovedBusDisappears() {
        val from = mapOf("OLD" to BusPose(1.0, 1.0, 0f), "A" to BusPose(0.0, 0.0, 0f))
        val to = mapOf("A" to BusPose(10.0, 10.0, 0f), "NEW" to BusPose(7.0, 7.0, 45f))

        val r = interpolateBuses(from, to, 0.5f)

        assertEquals(setOf("A", "NEW"), r.keys)
        assertEquals(5.0, r.getValue("A").latitude, 1e-9)
        assertEquals(BusPose(7.0, 7.0, 45f), r.getValue("NEW"))
    }

    @Test
    fun tIsClampedToZeroOne() {
        val from = mapOf("A" to BusPose(0.0, 0.0, 0f))
        val to = mapOf("A" to BusPose(10.0, 10.0, 0f))
        assertEquals(10.0, interpolateBuses(from, to, 5f).getValue("A").latitude, 1e-9)
        assertEquals(0.0, interpolateBuses(from, to, -3f).getValue("A").latitude, 1e-9)
    }

    @Test
    fun emptyInputsGiveEmptyResult() {
        assertTrue(interpolateBuses(emptyMap(), emptyMap(), 0.5f).isEmpty())
        assertTrue(interpolateBuses(mapOf("A" to BusPose(0.0, 0.0, 0f)), emptyMap(), 0.5f).isEmpty())
    }
}
