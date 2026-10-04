package com.vibra.bus.presentation.screens

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.vibra.bus.domain.brand.MascotPose
import com.vibra.bus.presentation.components.CelebrationOverlay
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.TextButton
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.WindowInsets
import com.vibra.bus.presentation.theme.readableOn
import com.vibra.bus.presentation.theme.appColors
import com.vibra.bus.presentation.theme.AppShape
import com.vibra.bus.presentation.components.PrimaryButton
import com.vibra.bus.presentation.components.AppTextField
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.Surface
import com.vibra.bus.presentation.components.AppTopBar
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
        var confetti by remember { mutableStateOf(0) }
        var okTick by remember { mutableStateOf(0) }
        val haptics = LocalHapticFeedback.current
        LaunchedEffect(lastResult) {
            when (lastResult) {
                is ScanResult.Charged, is ScanResult.Valid -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                is ScanResult.Error, is ScanResult.Invalid, is ScanResult.Expired, is ScanResult.Offline ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                null -> Unit
            }
            if (lastResult is ScanResult.Charged) confetti++
            if (lastResult is ScanResult.Valid) okTick++
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            topBar = {
                AppTopBar(
                    title = if (mode == ScanMode.PAY) "Cobro a bordo" else "Escanear QR",
                    onBack = if (navigator.canPop) ({ navigator.pop() }) else null,
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
                        style = MaterialTheme.typography.bodyMedium,
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
                            AppTextField(
                                value = manualCode,
                                onValueChange = { viewModel.onManualCodeChange(it) },
                                label = "Código del pasajero",
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            PrimaryButton(
                                text = "Cobrar",
                                onClick = { viewModel.onManualSubmit() },
                                enabled = manualCode.isNotBlank() && state !is ScanState.Validating,
                                modifier = Modifier.width(112.dp),
                            )
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
                        Surface(
                            shape = AppShape.Card,
                            color = MaterialTheme.colorScheme.inverseSurface,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.inverseOnSurface,
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = if (mode == ScanMode.PAY) "Cobrando pasaje…" else "Validando acceso…",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.inverseOnSurface,
                                )
                            }
                        }
                    }
                }

                CelebrationOverlay(trigger = confetti, pose = MascotPose.Celebrating)
                CelebrationOverlay(trigger = okTick, pose = MascotPose.Ok)

                // Banner de resultado (aparece/desaparece desde arriba)
                AnimatedVisibility(
                    visible = lastResult != null,
                    enter = slideInVertically(initialOffsetY = { -it }),
                    exit = slideOutVertically(targetOffsetY = { -it }),
                    modifier = Modifier.align(Alignment.TopCenter),
                ) {
                    lastResult?.let {
                        ResultBanner(it, onRetry = { viewModel.retryPending() }, onDismiss = { viewModel.dismissResult() })
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultBanner(result: ScanResult, onRetry: () -> Unit, onDismiss: () -> Unit) {
    val (icon, title, subtitle, bgColor) = when (result) {
        is ScanResult.Valid -> BannerData(
            icon = Icons.Default.CheckCircle,
            title = "Acceso autorizado: ${result.data.user.name}",
            subtitle = "${result.data.stop} · ${result.data.bus}",
            color = MaterialTheme.appColors.success,
        )
        is ScanResult.Charged -> BannerData(
            icon = Icons.Default.CheckCircle,
            title = "Cobrado ${formatCentavosCop(result.data.montoCentavos)}: ${result.data.pasajero ?: "pasajero"}",
            subtitle = "Saldo restante: ${formatCentavosCop(result.data.saldoRestante)}",
            color = MaterialTheme.appColors.success,
        )
        is ScanResult.Expired -> BannerData(
            icon = Icons.Default.HourglassEmpty,
            title = "QR expirado",
            subtitle = "Pide al pasajero que genere un código nuevo",
            color = MaterialTheme.appColors.warning,
        )
        is ScanResult.Invalid -> BannerData(
            icon = Icons.Default.Error,
            title = "QR inválido",
            subtitle = "El código no pertenece a esta aplicación",
            color = MaterialTheme.colorScheme.error,
        )
        is ScanResult.Error -> BannerData(
            icon = Icons.Default.Error,
            title = "No se pudo completar",
            subtitle = result.message,
            color = MaterialTheme.colorScheme.error,
        )
        is ScanResult.Offline -> BannerData(
            icon = Icons.Default.Error,
            title = "Sin conexión",
            subtitle = result.message,
            color = MaterialTheme.appColors.warning,
        )
    }

    // Texto siempre legible (AA) sobre el color semántico de la marca
    val onBanner = readableOn(bgColor)
    Column(modifier = Modifier.fillMaxWidth().background(bgColor)) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Assertive }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = onBanner,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = onBanner,
                style = MaterialTheme.typography.titleSmall,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = onBanner,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
    if (result is ScanResult.Offline) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancelar", color = onBanner)
            }
            TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Reintentar", color = onBanner, style = MaterialTheme.typography.labelLarge)
            }
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
