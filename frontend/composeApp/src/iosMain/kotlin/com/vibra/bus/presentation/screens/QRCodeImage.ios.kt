package com.vibra.bus.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.g0dkar.qrcode.QRCode
import org.jetbrains.skia.Image as SkiaImage
import androidx.compose.ui.graphics.toComposeImageBitmap

@Composable
actual fun QRCodeImage(content: String, modifier: Modifier) {
    val bitmap = remember(content) {
        try {
            val bytes = QRCode(content).render().getBytes()
            SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = "QR Code",
            modifier = modifier.fillMaxSize(),
        )
    }
}
