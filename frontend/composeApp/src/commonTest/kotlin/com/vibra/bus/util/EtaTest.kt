package com.vibra.bus.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EtaTest {

    @Test
    fun defaultSpeedIs30KmhWhenUnknown() {
        // 30 km/h = 500 m/min -> 2000 m = 4 min
        assertEquals(4, estimateEtaMinutes(2000.0, null))
    }

    @Test
    fun stoppedBusIsClampedToMinSpeedNotInfinite() {
        // 12 km/h = 200 m/min -> 1000 m = 5 min
        assertEquals(5, estimateEtaMinutes(1000.0, 0.0))
        assertEquals(5, estimateEtaMinutes(1000.0, -10.0))
    }

    @Test
    fun fastBusIsClampedToMaxSpeed() {
        // 50 km/h ~ 833 m/min -> 8333 m = 10 min, aunque reporte 200 km/h
        assertEquals(10, estimateEtaMinutes(8333.0, 200.0))
    }

    @Test
    fun etaIsAtLeastOneMinuteEvenAtZeroDistance() {
        assertEquals(1, estimateEtaMinutes(0.0, 30.0))
        assertEquals(1, estimateEtaMinutes(5.0, null))
    }

    @Test
    fun etaGrowsWithDistance() {
        assertTrue(estimateEtaMinutes(5000.0, 30.0) > estimateEtaMinutes(1000.0, 30.0))
    }

    @Test
    fun smoothSpeedFirstSampleIsInstant() {
        // 100 m en 10 s = 10 m/s = 36 km/h
        assertEquals(36.0, smoothSpeedKmh(null, 100.0, 10.0), 0.001)
    }

    @Test
    fun smoothSpeedBlendsWithPrevious() {
        // previo 20, instantanea 36 -> 20*0.6 + 36*0.4 = 26.4
        assertEquals(26.4, smoothSpeedKmh(20.0, 100.0, 10.0), 0.001)
    }

    @Test
    fun smoothSpeedIgnoresNonPositiveDuration() {
        assertEquals(20.0, smoothSpeedKmh(20.0, 100.0, 0.0), 0.0)
        assertEquals(30.0, smoothSpeedKmh(null, 100.0, -5.0), 0.0)
    }

    @Test
    fun formatEtaCases() {
        assertEquals("Calculando…", formatEta(null))
        assertEquals("Menos de 1 min", formatEta(1))
        assertEquals("Menos de 1 min", formatEta(0))
        assertEquals("7 min", formatEta(7))
    }
}
