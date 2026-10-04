package com.vibra.bus.presentation.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

@Composable
actual fun platformAppActive(): Boolean = true

@Composable
actual fun platformReduceMotion(): Boolean = remember { UIAccessibilityIsReduceMotionEnabled() }

@Composable
actual fun platformLowTier(): Boolean = remember {
    NSProcessInfo.processInfo.physicalMemory < 3_000_000_000UL
}

@Composable
actual fun platformMidTier(): Boolean = remember {
    NSProcessInfo.processInfo.physicalMemory < 4_000_000_000UL
}
