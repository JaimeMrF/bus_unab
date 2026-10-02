package com.vibra.bus.presentation.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import qrcode.QRCode

/** El QR se codifica y dibuja en un hilo de fondo; mientras tanto no se pinta nada (sin bloquear la UI). */
@Composable
actual fun QRCodeImage(content: String, modifier: Modifier) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, content) {
        value = withContext(Dispatchers.Default) {
            try {
                (QRCode.ofSquares().build(content).render().nativeImage() as? Bitmap)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = "Código QR",
            modifier = modifier.fillMaxSize(),
        )
    }
}
