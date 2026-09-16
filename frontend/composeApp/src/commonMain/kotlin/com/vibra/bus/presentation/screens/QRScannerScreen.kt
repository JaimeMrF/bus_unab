package com.vibra.bus.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.presentation.viewmodel.QRScannerViewModel
import com.vibra.bus.presentation.viewmodel.ScanMode
import com.vibra.bus.presentation.viewmodel.ScanResult
import com.vibra.bus.presentation.viewmodel.ScanState
import com.vibra.bus.presentation.viewmodel.formatCentavosCop
import org.koin.compose.viewmodel.koinViewModel

class QRScannerScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<QRScannerViewModel>()
        val state by viewModel.state.collectAsState()
        val lastResult by viewModel.lastResult.collectAsState()
        val mode by viewModel.mode.collectAsState()
        val manualCode by viewModel.manualCode.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (mode == ScanMode.PAY) "Cobro a bordo (mPOS)" else "Escanear QR",
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    },
                    navigationIcon = {
                        if (navigator.canPop) {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(Icons.AutoMirrored.Default.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                // Cámara siempre visible
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Toggle de modo: validación de acceso vs cobro de pasaje
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = mode == ScanMode.ACCESS,
                            onClick = { viewModel.setMode(ScanMode.ACCESS) },
                            label = { Text("Acceso") },
                        )
                        FilterChip(
                            selected = mode == ScanMode.PAY,
                            onClick = { viewModel.setMode(ScanMode.PAY) },
                            label = { Text("Cobro") },
                        )
                    }
                    Text(
                        text = if (mode == ScanMode.PAY)
                            "Escanea el QR de pago del pasajero, o digita su código manualmente"
                        else
                            "Escanea el código QR del pasajero para validar su acceso",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    QRScannerView(
                        modifier = Modifier.fillMaxWidth().height(320.dp),
                        onResult = { viewModel.onQrScanned(it) },
                    )

                    // Entrada manual del código (fallback si la cámara no lee)
                    if (mode == ScanMode.PAY) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = manualCode,
                                onValueChange = { viewModel.onManualCodeChange(it) },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Pegar/escribir código del pasajero", fontSize = 12.sp) },
                                singleLine = true,
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.onManualSubmit() },
                                enabled = manualCode.isNotBlank() && state !is ScanState.Validating,
                            ) {
                                Text("Cobrar")
                            }
                        }
                    }
                }

                // Overlay de validando
                if (state is ScanState.Validating) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .padding(horizontal = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            modifier = Modifier
                                .background(
                                    color = Color.Black.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = if (mode == ScanMode.PAY) "Cobrando pasaje..." else "Validando acceso...",
                                color = Color.White,
                                fontSize = 15.sp,
                            )
                        }
                    }
                }

                // Banner de resultado (aparece/desaparece desde arriba)
                AnimatedVisibility(
                    visible = lastResult != null,
                    enter = slideInVertically(initialOffsetY = { -it }),
                    exit = slideOutVertically(targetOffsetY = { -it }),
                    modifier = Modifier.align(Alignment.TopCenter),
                ) {
                    lastResult?.let { ResultBanner(it) }
                }
            }
        }
    }
}

@Composable
private fun ResultBanner(result: ScanResult) {
    val (icon, title, subtitle, bgColor) = when (result) {
        is ScanResult.Valid -> BannerData(
            icon = Icons.Default.CheckCircle,
            title = "Acceso autorizado — ${result.data.user.name}",
            subtitle = "${result.data.stop} · ${result.data.bus}",
            color = Color(0xFF2E7D32),
        )
        is ScanResult.Charged -> BannerData(
            icon = Icons.Default.CheckCircle,
            title = "Cobrado ${formatCentavosCop(result.data.montoCentavos)} — ${result.data.pasajero ?: "pasajero"}",
            subtitle = "Saldo restante: ${formatCentavosCop(result.data.saldoRestante)}",
            color = Color(0xFF1B5E20),
        )
        is ScanResult.Expired -> BannerData(
            icon = Icons.Default.HourglassEmpty,
            title = "QR expirado",
            subtitle = "Pide al pasajero que genere un código nuevo",
            color = Color(0xFFE65100),
        )
        is ScanResult.Invalid -> BannerData(
            icon = Icons.Default.Error,
            title = "QR inválido",
            subtitle = "El código no pertenece a esta aplicación",
            color = Color(0xFFC62828),
        )
        is ScanResult.Error -> BannerData(
            icon = Icons.Default.Error,
            title = "Error de validación",
            subtitle = result.message,
            color = Color(0xFFC62828),
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

private data class BannerData(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val color: Color,
)

@Composable
expect fun QRScannerView(
    modifier: Modifier = Modifier,
    onResult: (String) -> Unit,
)
