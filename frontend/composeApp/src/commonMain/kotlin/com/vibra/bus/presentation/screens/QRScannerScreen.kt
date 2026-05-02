package com.vibra.bus.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.vibra.bus.data.model.QrValidateData
import com.vibra.bus.presentation.viewmodel.QRScannerViewModel
import com.vibra.bus.presentation.viewmodel.ScanState
import org.koin.compose.viewmodel.koinViewModel

class QRScannerScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<QRScannerViewModel>()
        val state by viewModel.state.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Escanear QR", color = MaterialTheme.colorScheme.onPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                when (val s = state) {
                    is ScanState.Scanning -> ScanningView(viewModel)
                    is ScanState.Validating -> ValidatingView()
                    is ScanState.Valid -> ValidResultView(s.data, onReset = { viewModel.reset() })
                    is ScanState.Expired -> ResultErrorView(
                        title = "QR Expirado",
                        message = "El código ya no es válido. Pide al estudiante que genere uno nuevo.",
                        onReset = { viewModel.reset() },
                    )
                    is ScanState.Invalid -> ResultErrorView(
                        title = "QR Inválido",
                        message = "El código escaneado no pertenece a esta aplicación.",
                        onReset = { viewModel.reset() },
                    )
                    is ScanState.Error -> ResultErrorView(
                        title = "Error de validación",
                        message = s.message,
                        onReset = { viewModel.reset() },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanningView(viewModel: QRScannerViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Escanea el código QR del estudiante para validar su acceso",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp),
        )
        QRScannerView(
            modifier = Modifier.fillMaxWidth().height(400.dp),
            onResult = { viewModel.onQrScanned(it) },
        )
    }
}

@Composable
private fun ValidatingView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 5.dp,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Validando acceso...",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ValidResultView(data: QrValidateData, onReset: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(80.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Acceso Autorizado",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50),
        )
        Spacer(Modifier.height(20.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                InfoRow(label = "Estudiante", value = data.user.name)
                Spacer(Modifier.height(8.dp))
                InfoRow(label = "Email", value = data.user.email)
                Spacer(Modifier.height(8.dp))
                InfoRow(label = "Bus", value = data.bus)
                Spacer(Modifier.height(8.dp))
                InfoRow(label = "Parada", value = data.stop)
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text("Escanear otro", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ResultErrorView(title: String, message: String, onReset: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(80.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text("Volver a escanear", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
expect fun QRScannerView(
    modifier: Modifier = Modifier,
    onResult: (String) -> Unit,
)
