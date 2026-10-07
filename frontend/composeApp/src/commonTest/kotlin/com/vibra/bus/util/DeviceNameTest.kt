package com.vibra.bus.util

import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceNameTest {
    @Test fun trimsCollapsesAndLimitsTo60() {
        assertEquals("Samsung SM-A515F", sanitizeDeviceName("  Samsung   SM-A515F ", "Android"))
        assertEquals(60, sanitizeDeviceName("x".repeat(100), "Android").length)
    }

    @Test fun blankFallsBack() {
        assertEquals("Android", sanitizeDeviceName("   ", "Android"))
    }
}
