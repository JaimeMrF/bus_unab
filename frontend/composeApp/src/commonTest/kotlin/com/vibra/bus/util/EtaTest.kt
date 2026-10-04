package com.vibra.bus.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EtaTest {
    @Test fun defaultSpeedGivesExpectedMinutes() {
        // 30 km/h = 500 m/min
        assertEquals(4, estimateEtaMinutes(2000.0, null))
        assertEquals(1, estimateEtaMinutes(10.0, null))
    }

    @Test fun speedIsClampedToUrbanRange() {
        assertEquals(estimateEtaMinutes(1000.0, 12.0), estimateEtaMinutes(1000.0, 0.0))
        assertEquals(estimateEtaMinutes(1000.0, 50.0), estimateEtaMinutes(1000.0, 200.0))
    }

    @Test fun smoothingBlendsPreviousAndInstant() {
        val v = smoothSpeedKmh(20.0, movedMeters = 100.0, seconds = 10.0) // instant 36 km/h
        assertTrue(v > 20.0 && v < 36.0)
        assertEquals(36.0, smoothSpeedKmh(null, 100.0, 10.0), 0.001)
    }

    @Test fun formatting() {
        assertEquals("Calculando…", formatEta(null))
        assertEquals("Menos de 1 min", formatEta(1))
        assertEquals("7 min", formatEta(7))
    }
}
