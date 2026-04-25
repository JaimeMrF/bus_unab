package com.vibra.bus.util

actual class QRScannerManager {
    actual fun startScanning(onResult: (String) -> Unit, onError: (String) -> Unit) {
        // iOS implementation uses AVFoundation AVCaptureSession
        // Implemented via UIViewControllerRepresentable bridge in iosApp
    }

    actual fun stopScanning() {}
}
