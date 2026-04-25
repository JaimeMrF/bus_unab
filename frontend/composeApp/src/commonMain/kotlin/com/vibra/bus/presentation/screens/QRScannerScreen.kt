package com.vibra.bus.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.data.model.QRPayload
import com.vibra.bus.presentation.theme.AppColors
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

class QRScannerScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Escanear QR", color = AppColors.White) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, "Volver", tint = AppColors.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.DarkHeader),
                )
            },
            containerColor = AppColors.PrimaryBg,
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Escanea el código del usuario para validar su acceso",
                    color = AppColors.GrayText,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                )

                QRScannerView(
                    modifier = Modifier.fillMaxWidth().height(360.dp),
                    onResult = { content -> handleQRResult(content) },
                )
            }
        }
    }

    private fun handleQRResult(content: String): QRValidationResult {
        return try {
            val payload = Json.decodeFromString<QRPayload>(content)
            val now = Clock.System.now().toEpochMilliseconds()
            val age = now - payload.ts
            if (age > 60_000) QRValidationResult.Expired
            else QRValidationResult.Valid(payload)
        } catch (e: Exception) {
            QRValidationResult.Invalid
        }
    }
}

sealed class QRValidationResult {
    data class Valid(val payload: QRPayload) : QRValidationResult()
    data object Expired : QRValidationResult()
    data object Invalid : QRValidationResult()
}

@Composable
expect fun QRScannerView(
    modifier: Modifier = Modifier,
    onResult: (String) -> Unit,
)
