package com.vibra.bus.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureSession
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
