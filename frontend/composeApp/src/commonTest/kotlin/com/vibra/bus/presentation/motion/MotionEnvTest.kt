package com.vibra.bus.presentation.motion

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MotionEnvTest {

    @Test fun normalDeviceRunsEverything() {
        val env = MotionEnv(reduceMotion = false, lowTier = false, appActive = true)
        assertTrue(env.animate)
        assertTrue(env.ambient)
    }

    @Test fun reduceMotionDisablesAllMotion() {
        val env = MotionEnv(reduceMotion = true, lowTier = false, appActive = true)
        assertFalse(env.animate)
        assertFalse(env.ambient)
    }

    @Test fun lowTierKeepsShortTransitionsButNoAmbientEffects() {
        val env = MotionEnv(reduceMotion = false, lowTier = true, appActive = true)
        assertTrue(env.animate)
        assertFalse(env.ambient)
    }

    @Test fun backgroundedAppPausesLoops() {
        val env = MotionEnv(reduceMotion = false, lowTier = false, appActive = false)
        assertFalse(env.ambient)
    }
}
