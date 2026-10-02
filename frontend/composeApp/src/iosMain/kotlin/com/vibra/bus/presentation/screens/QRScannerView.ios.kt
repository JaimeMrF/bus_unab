package com.vibra.bus.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun QRScannerView(modifier: Modifier, onResult: (String) -> Unit) {
    // iOS QR scanner uses AVFoundation
    // Full implementation requires a UIViewController bridge
    UIKitView(
        factory = {
            val view = UIView()
            view.backgroundColor = platform.UIKit.UIColor.blackColor
            view
        },
        modifier = modifier,
    )
}
