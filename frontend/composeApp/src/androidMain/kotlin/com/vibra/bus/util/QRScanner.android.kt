package com.vibra.bus.util

import android.content.Context

actual class QRScannerManager(private val context: Context) {
    private var isScanning = false

    actual fun startScanning(onResult: (String) -> Unit, onError: (String) -> Unit) {
        isScanning = true
        // Camera implementation is in QRScannerView composable
    }

    actual fun stopScanning() {
        isScanning = false
    }
}
