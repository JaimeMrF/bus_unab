package com.vibra.bus.util

expect class QRScannerManager {
    fun startScanning(onResult: (String) -> Unit, onError: (String) -> Unit)
    fun stopScanning()
}
