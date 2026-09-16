package com.vibra.bus.presentation.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.data.model.WalletTx
import com.vibra.bus.presentation.viewmodel.WalletViewModel
import com.vibra.bus.presentation.viewmodel.formatCentavosCop
import org.koin.compose.viewmodel.koinViewModel

class WalletScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
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

        LaunchedEffect(Unit) { viewModel.refresh() }

        LaunchedEffect(message) {
            message?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.consumeMessage()
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = 18.dp, bottom = 24.dp,
                ),
            ) {
                // ── Saldo ───────────────────────────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(18.dp)) {
                            Text(
                                "Saldo disponible",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = wallet?.let { formatCentavosCop(it.balanceCentavos) } ?: "—",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            if (loading && wallet == null) {
                                Spacer(Modifier.height(8.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }
                }

                // ── QR de pago dinámico ─────────────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Pagar a bordo (QR dinámico)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "El conductor escanea este código y se descuenta de tu saldo al instante.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(12.dp))

                            if (!payVisible) {
                                Button(onClick = {
                                    payVisible = true
                                    viewModel.showPayQr()
                                }) { Text("Mostrar QR de pago") }
                            } else {
                                val qr = payQr
                                if (qr == null) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.padding(vertical = 24.dp),
                                    )
                                    Text(
                                        "Generando código…",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(220.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White)
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        QRCodeImage(content = qr.qr, modifier = Modifier.fillMaxSize())
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        "Pasaje: ${formatCentavosCop(qr.montoCentavos)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = if (payCountdown > 10)
                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                                    else
                                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                                    shape = RoundedCornerShape(20.dp),
                                                )
                                                .padding(horizontal = 14.dp, vertical = 6.dp),
                                        ) {
                                            Text(
                                                "Expira en ${payCountdown}s",
                                                color = if (payCountdown > 10)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        TextButton(onClick = { viewModel.regeneratePayQr() }) {
                                            Text("Regenerar ahora")
                                        }
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                OutlinedButton(onClick = {
                                    payVisible = false
                                    viewModel.hidePayQr()
                                }) { Text("Ocultar QR") }
                            }
                        }
                    }
                }

                // ── Recarga (mock) ──────────────────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(
                                "Recargar saldo",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "Recarga simulada (solo entorno de pruebas — proveedor de pagos pendiente).",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf(1_000, 2_000, 3_000, 5_000).forEach { cop ->
                                    val selected = rechargeAmount == cop
                                    val shape = RoundedCornerShape(10.dp)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                shape,
                                            )
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        TextButton(
                                            onClick = { rechargeAmount = cop },
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                                horizontal = 4.dp, vertical = 0.dp,
                                            ),
                                        ) {
                                            Text(
                                                "$cop",
                                                fontSize = 13.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    viewModel.recharge(rechargeAmount * 100) // COP → centavos
                                },
                                enabled = !loading,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Recargar ${formatCentavosCop(rechargeAmount * 100L)}")
                            }
                        }
                    }
                }

                // ── QR de viaje (acceso legacy) ─────────────────────
                item {
                    OutlinedButton(
                        onClick = { navigator.push(MyQRScreen()) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Ver QR de viaje (reserva de puesto)")
                    }
                }

                // ── Historial ───────────────────────────────────────
                item {
                    Text(
                        "Movimientos recientes",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                val txs = wallet?.transactions ?: emptyList()
                if (txs.isEmpty()) {
                    item {
                        Text(
                            if (loading) "Cargando movimientos…" else "Aún no tienes movimientos.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(txs, key = { it.id }) { tx -> TxRow(tx) }
                }
            }
        }
    }
}

@Composable
private fun TxRow(tx: WalletTx) {
    val positive = tx.tipo == "credit"
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (positive) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (positive) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                tx.contraparte?.replaceFirstChar { it.uppercase() } ?: tx.tipo,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                (tx.createdAt ?: "").take(16).replace('T', ' '),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = (if (positive) "+" else "−") + formatCentavosCop(tx.montoCentavos),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            Text(
                "saldo " + formatCentavosCop(tx.balanceAfter),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
        modifier = Modifier.padding(vertical = 6.dp),
    )
}
