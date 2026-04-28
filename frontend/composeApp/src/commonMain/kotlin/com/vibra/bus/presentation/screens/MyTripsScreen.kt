package com.vibra.bus.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import com.vibra.bus.data.model.RequestInfo
import com.vibra.bus.presentation.components.EmptyState
import com.vibra.bus.presentation.components.ShimmerBox
import com.vibra.bus.presentation.theme.VibraBusShapes
import com.vibra.bus.presentation.viewmodel.MyTripsViewModel
import com.vibra.bus.util.UiState
import org.koin.compose.viewmodel.koinViewModel

class MyTripsScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<MyTripsViewModel>()
        val tripsState by viewModel.tripsState.collectAsState()
        val snackbarMsg by viewModel.snackbarMessage.collectAsState()
        val snackbarState = remember { SnackbarHostState() }
        var isVisible by remember { mutableStateOf(false) }

        val headerScale by animateFloatAsState(
            targetValue = if (isVisible) 1f else 0.9f,
            animationSpec = tween(durationMillis = 600),
            label = "header_scale"
        )

        LaunchedEffect(snackbarMsg) {
            snackbarMsg?.let {
                snackbarState.showSnackbar(it)
                viewModel.consumeSnackbar()
            }
        }

        LaunchedEffect(Unit) { isVisible = true }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarState) },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {

                // ── Header ───────────────────────────────────────────────────
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(durationMillis = 600)
                    ) + fadeIn(animationSpec = tween(durationMillis = 600))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                            .scale(headerScale),
                    ) {
                        Column {
                            Text(
                                text = "Mis Viajes",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = "Historial de solicitudes",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // ── Contenido ─────────────────────────────────────────────────
                // Movemos AnimatedVisibility para que envuelva al PullToRefreshBox
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(durationMillis = 800, delayMillis = 300))
                ) {
                    PullToRefreshBox(
                        isRefreshing = false,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            when (val state = tripsState) {
                                is UiState.Loading -> {
                                    LazyColumn(
                                        contentPadding = PaddingValues(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(5) {
                                            ShimmerBox(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(120.dp)
                                                    .clip(VibraBusShapes.Card)
                                            )
                                        }
                                    }
                                }
                                is UiState.Success -> {
                                    if (state.data.isEmpty()) {
                                        EmptyState(
                                            message = "Sin viajes activos",
                                            subtitle = "Solicita un bus desde la pantalla de inicio",
                                        )
                                    } else {
                                        LazyColumn(contentPadding = PaddingValues(16.dp)) {
                                            items(state.data, key = { it.id }) { trip ->
                                                TripCard(
                                                    trip = trip,
                                                    onCancel = { viewModel.cancelTrip(trip.bus.id) },
                                                )
                                            }
                                        }
                                    }
                                }
                                is UiState.Error -> EmptyState(message = state.message)
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TripCard(trip: RequestInfo, onCancel: () -> Unit) {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart && trip.status == "pending") {
                    onCancel(); true
                } else false
            }
        )

        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(end = 20.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Text(
                        "Cancelar",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            modifier = Modifier.padding(vertical = 6.dp),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(14.dp),
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    ),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = trip.bus.name,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f),
                        )
                        StatusChip(status = trip.status)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Parada: ${trip.stop.name}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Text(
                        trip.stop.address,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    if (trip.status == "pending") {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Desliza para cancelar →",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun StatusChip(status: String) {
        val (bg, textColor, label) = when (status) {
            "pending" -> Triple(
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f),
                MaterialTheme.colorScheme.onSecondaryContainer,
                "Pendiente"
            )
            "active" -> Triple(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                MaterialTheme.colorScheme.primary,
                "Activo"
            )
            else -> Triple(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.onSurfaceVariant,
                "Completado"
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(bg)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
