package com.vibra.bus.presentation.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import qrcode.QRCode

@Composable
actual fun QRCodeImage(content: String, modifier: Modifier) {
    val bitmap = remember(content) {
        try {
            QRCode.ofSquares()
                .build(content)
                .render()
                .nativeImage() as? Bitmap
        } catch (e: Exception) {
            null
        }
    }
    bitmap?.asImageBitmap()?.let {
        Image(
            bitmap = it,
            contentDescription = "QR Code",
            modifier = modifier.fillMaxSize(),
        )
    }
}
