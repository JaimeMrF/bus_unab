package com.vibra.bus.presentation.screens

import com.vibra.bus.presentation.components.PillTone
import com.vibra.bus.presentation.components.StatusPill
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.TextButton
import com.vibra.bus.presentation.components.PrimaryButton
import com.vibra.bus.presentation.components.AppCard
import com.vibra.bus.presentation.components.ShimmerList
import com.vibra.bus.presentation.components.AppTopBar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.Card
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.data.model.RequestInfo
import com.vibra.bus.data.model.StopDto
import com.vibra.bus.presentation.components.EmptyState
import com.vibra.bus.util.AppSettings
import org.koin.compose.koinInject
import com.vibra.bus.presentation.theme.AppShape
import com.vibra.bus.presentation.viewmodel.MyTripsViewModel
import com.vibra.bus.util.UiState
import org.koin.compose.viewmodel.koinViewModel

class MyTripsScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<MyTripsViewModel>()
        val settings  = koinInject<AppSettings>()
        val tripsState by viewModel.tripsState.collectAsState()
        val isRefreshing by viewModel.isRefreshing.collectAsState()
        val snackbarMsg by viewModel.snackbarMessage.collectAsState()
        val snackbarState = remember { SnackbarHostState() }

        LaunchedEffect(snackbarMsg) {
            snackbarMsg?.let {
                snackbarState.showSnackbar(it)
                viewModel.consumeSnackbar()
            }
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            snackbarHost = { SnackbarHost(snackbarState) },
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { AppTopBar(title = "Mis viajes", subtitle = "Historial de solicitudes") },
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {

                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            when (val state = tripsState) {
                                is UiState.Loading -> ShimmerList(count = 4, itemHeight = 120.dp)
                                is UiState.Success -> {
                                    if (state.data.isEmpty()) {
                                        EmptyState(
                                            message = "Sin viajes activos",
                                            subtitle = "Solicita un bus desde la pantalla de inicio",
                                        )
                                    } else {
                                        LazyColumn(contentPadding = PaddingValues(16.dp)) {
                                            items(state.data, key = { it.id }) { trip ->
                                                val canResume = trip.status == "pending" &&
                                                    settings.hasActiveTracking() &&
                                                    settings.trackingPlate == trip.bus.plate
                                                TripCard(
                                                    trip = trip,
                                                    onCancel = { viewModel.cancelTrip(trip.bus.id) },
                                                    onResume = if (canResume) {
                                                        {
                                                            val stop = StopDto(
                                                                id           = settings.trackingStopId,
                                                                name         = settings.trackingStopName,
                                                                address      = settings.trackingStopAddress,
                                                                latitude     = settings.trackingStopLat,
                                                                longitude    = settings.trackingStopLng,
                                                                radiusMeters = 50,
                                                            )
                                                            startBusTracking(trip.bus.plate, stop.latitude, stop.longitude, stop.name)
                                                            navigator.push(WaitingBusScreen(trip.bus.plate, stop))
                                                        }
                                                    } else null,
                                                )
                                            }
                                        }
                                    }
                                }
                                is UiState.Error -> EmptyState(
                                    message = "No pudimos cargar tus viajes",
                                    subtitle = state.message,
                                    ctaLabel = "Reintentar",
                                    onCtaClick = { viewModel.refresh() },
                                )
                                else -> {}
                            }
                        }
                    }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TripCard(trip: RequestInfo, onCancel: () -> Unit, onResume: (() -> Unit)? = null) {
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
                        .background(MaterialTheme.colorScheme.errorContainer, AppShape.Card)
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
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = trip.bus.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        StatusChip(status = trip.status)
                    }
                    Text(
                        "Parada: ${trip.stop.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        trip.stop.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (trip.status == "pending") {
                        Spacer(Modifier.height(8.dp))
                        if (onResume != null) {
                            PrimaryButton(
                                text = "Ver en mapa",
                                leadingIcon = Icons.Default.DirectionsBus,
                                onClick = onResume,
                            )
                        }
                        // Alternativa explícita al gesto de deslizar (accesibilidad)
                        TextButton(
                            onClick = onCancel,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Text(
                                "Cancelar viaje",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun StatusChip(status: String) {
        when (status) {
            "pending" -> StatusPill("Pendiente", PillTone.Warning, showDot = true)
            "active" -> StatusPill("Activo", PillTone.Success, showDot = true)
            else -> StatusPill("Completado", PillTone.Neutral)
        }
    }
}
