package com.vibra.bus.presentation.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BusMotionTest {
    @Test fun headingTakesShortestWay() {
        assertEquals(0f, lerpHeading(350f, 10f, 0.5f), 0.001f)
        assertEquals(355f, lerpHeading(350f, 10f, 0.25f), 0.001f)
        assertEquals(350f, lerpHeading(10f, 350f, 1f), 0.001f)
    }

    @Test fun headingStaysInRange() {
        for (a in 0..359 step 17) for (b in 0..359 step 23) {
            val h = lerpHeading(a.toFloat(), b.toFloat(), 0.37f)
            assertTrue(h >= 0f && h < 360f, "$a->$b = $h")
        }
    }

    @Test fun interpolationIsLinearAndHandlesNewAndGoneBuses() {
        val from = mapOf("A" to BusPose(0.0, 0.0, 0f), "OLD" to BusPose(1.0, 1.0, 0f))
        val to = mapOf("A" to BusPose(2.0, 4.0, 90f), "NEW" to BusPose(5.0, 5.0, 45f))
        val mid = interpolateBuses(from, to, 0.5f)
        assertEquals(1.0, mid.getValue("A").latitude, 1e-9)
        assertEquals(2.0, mid.getValue("A").longitude, 1e-9)
        assertEquals(45f, mid.getValue("A").heading, 0.001f)
        assertEquals(BusPose(5.0, 5.0, 45f), mid["NEW"])
        assertTrue("OLD" !in mid)
    }
}

class BusSpriteKeyTest {
    @Test fun keysDifferByIconUrlAndColor() {
        val a = BusSpriteKey(1, 2, 3, BusMapState.Available, BusIcon.Classic, 96, null)
        assertEquals(a, a.copy())
        assertTrue(a != a.copy(iconUrl = "https://x/i.png"))
        assertTrue(a != a.copy(body = 9))
    }
}
