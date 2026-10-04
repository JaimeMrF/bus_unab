package com.vibra.bus.presentation.screens

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.vibra.bus.domain.brand.MascotPose
import com.vibra.bus.presentation.components.CelebrationOverlay
import com.vibra.bus.presentation.components.BrandMascot
import com.vibra.bus.presentation.security.ProtectedQrDisplay
import androidx.compose.foundation.lazy.rememberLazyListState
import com.vibra.bus.presentation.motion.CountdownRing
import com.vibra.bus.presentation.motion.CountUpText
import com.vibra.bus.presentation.motion.parallaxCollapse
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.data.model.WalletTx
import com.vibra.bus.presentation.components.AppCard
import com.vibra.bus.presentation.components.AppTopBar
import com.vibra.bus.presentation.components.PrimaryButton
import com.vibra.bus.presentation.components.QRCodeImage
import com.vibra.bus.presentation.components.SecondaryButton
import com.vibra.bus.presentation.components.ShimmerBox
import com.vibra.bus.presentation.theme.AppShape
import com.vibra.bus.presentation.theme.appColors
import com.vibra.bus.presentation.viewmodel.WalletViewModel
import com.vibra.bus.presentation.viewmodel.formatCentavosCop
import org.koin.compose.viewmodel.koinViewModel

class WalletScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<WalletViewModel>()
        val wallet by viewModel.wallet.collectAsState()
        val loading by viewModel.loading.collectAsState()
        val message by viewModel.message.collectAsState()
        val payQr by viewModel.payQr.collectAsState()
        val payCountdown by viewModel.payCountdown.collectAsState()

        val snackbarHostState = remember { SnackbarHostState() }
        var payVisible by remember { mutableStateOf(false) }
        var rechargeAmount by remember { mutableStateOf(5_000) } // COP
        var confetti by remember { mutableStateOf(0) }
        val haptics = LocalHapticFeedback.current
        val listState = rememberLazyListState()

        LaunchedEffect(Unit) { viewModel.refresh() }
        LaunchedEffect(message) {
            message?.let {
                if (it.startsWith("Recarga aplicada")) {
                    confetti++
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                snackbarHostState.showSnackbar(it)
                viewModel.consumeMessage()
            }
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = { AppTopBar(title = "Wallet") },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            ) {
                // ── Saldo ───────────────────────────────────────────
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().parallaxCollapse(
                            offset = {
                                if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat()
                                else 4000f
                            },
                        ),
                        shape = AppShape.CardLarge,
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Column(Modifier.fillMaxWidth().padding(20.dp)) {
                            Text(
                                "Saldo disponible",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.height(4.dp))
                            if (wallet == null && loading) {
                                ShimmerBox(height = 40.dp)
                            } else {
                                val current = wallet
                                if (current == null) {
                                    Text(
                                        text = "—",
                                        style = MaterialTheme.typography.displayMedium,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                } else {
                                    CountUpText(
                                        target = current.balanceCentavos,
                                        style = MaterialTheme.typography.displayMedium,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        format = { formatCentavosCop(it) },
                                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                    )
                                }
                            }
                        }
                    }
                }

                // ── QR de pago dinámico ─────────────────────────────
                item {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            BrandMascot(pose = MascotPose.Phone, size = 72.dp)
                            Text(
                                "Pagar a bordo",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(
                                "El conductor escanea este código y se descuenta de tu saldo al instante.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(4.dp))

                            if (payVisible) ProtectedQrDisplay()
                            if (!payVisible) {
                                PrimaryButton(
                                    text = "Mostrar QR de pago",
                                    leadingIcon = Icons.Default.QrCode,
                                    onClick = {
                                        payVisible = true
                                        viewModel.showPayQr()
                                    },
                                )
                            } else {
                                val qr = payQr
                                if (qr == null) {
                                    ShimmerBox(height = 220.dp)
                                    Text(
                                        "Generando código…",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else {
                                    // El QR siempre sobre blanco para máximo contraste de lectura
                                    Box(
                                        modifier = Modifier
                                            .size(220.dp)
                                            .clip(AppShape.QRContainer)
                                            .background(Color.White)
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        QRCodeImage(content = qr.qr, modifier = Modifier.fillMaxSize())
                                    }
                                    Text(
                                        "Pasaje: ${formatCentavosCop(qr.montoCentavos)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CountdownRing(remaining = payCountdown, total = 60, size = 44.dp, strokeWidth = 4.dp) {
                                            Text(
                                                "$payCountdown",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "segundos",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        TextButton(
                                            onClick = { viewModel.regeneratePayQr() },
                                            modifier = Modifier.heightIn(min = 48.dp),
                                        ) { Text("Regenerar") }
                                    }
                                }
                                SecondaryButton(
                                    text = "Ocultar QR",
                                    onClick = {
                                        payVisible = false
                                        viewModel.hidePayQr()
                                    },
                                )
                            }
                        }
                    }
                }

                // ── Recarga (entorno de pruebas) ────────────────────
                item {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "Recargar saldo",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(
                                "Recarga simulada (solo entorno de pruebas, proveedor de pagos pendiente).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf(1_000, 2_000, 3_000, 5_000).forEach { cop ->
                                    AmountChip(
                                        label = "$cop",
                                        selected = rechargeAmount == cop,
                                        onClick = { rechargeAmount = cop },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                            PrimaryButton(
                                text = "Recargar ${formatCentavosCop(rechargeAmount * 100L)}",
                                loading = loading,
                                onClick = { viewModel.recharge(rechargeAmount * 100) },
                            )
                        }
                    }
                }

                // ── QR de viaje (acceso legacy) ─────────────────────
                item {
                    SecondaryButton(
                        text = "Ver QR de viaje (reserva de puesto)",
                        onClick = { navigator.push(MyQRScreen()) },
                    )
                }

                // ── Historial ───────────────────────────────────────
                item {
                    Text(
                        "Movimientos recientes",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.semantics { heading() },
                    )
                }

                val txs = wallet?.transactions ?: emptyList()
                when {
                    txs.isNotEmpty() -> items(txs, key = { it.id }, contentType = { "tx" }) { tx -> TxRow(tx) }
                    loading -> item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(3) { ShimmerBox(height = 44.dp) }
                        }
                    }
                    else -> item {
                        Text(
                            "Aún no tienes movimientos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            CelebrationOverlay(trigger = confetti, modifier = Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun AmountChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = AppShape.Chip,
        color = if (selected) scheme.primaryContainer else scheme.surfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TxRow(tx: WalletTx) {
    val positive = tx.tipo == "credit"
    val scheme = MaterialTheme.colorScheme
    val amount = (if (positive) "+" else "−") + formatCentavosCop(tx.montoCentavos)
    val title = tx.contraparte?.replaceFirstChar { it.uppercase() } ?: tx.tipo
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "$title, ${if (positive) "ingreso" else "egreso"} $amount"
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(if (positive) scheme.primaryContainer else scheme.errorContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (positive) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (positive) scheme.onPrimaryContainer else scheme.onErrorContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                Text(
                    (tx.createdAt ?: "").take(16).replace('T', ' '),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (positive) MaterialTheme.appColors.success.let { com.vibra.bus.presentation.theme.ensureContrast(it, scheme.background) } else scheme.error,
                )
                Text(
                    "saldo " + formatCentavosCop(tx.balanceAfter),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(color = scheme.outlineVariant)
    }
}
