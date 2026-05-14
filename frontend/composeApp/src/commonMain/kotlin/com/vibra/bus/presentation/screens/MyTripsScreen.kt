package com.vibra.bus.presentation.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.data.model.RequestInfo
import com.vibra.bus.data.model.StopDto
import com.vibra.bus.presentation.components.EmptyState
import com.vibra.bus.util.AppSettings
import org.koin.compose.koinInject
import com.vibra.bus.presentation.theme.VibraBusShapes
import com.vibra.bus.presentation.viewmodel.MyTripsViewModel
import com.vibra.bus.util.UiState
import org.koin.compose.viewmodel.koinViewModel
import vibrabus.composeapp.generated.resources.Res
import vibrabus.composeapp.generated.resources.buho_muy_triste
import vibrabus.composeapp.generated.resources.buho_un_poco_triste

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
            snackbarHost = { SnackbarHost(snackbarState) },
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text       = "Mis Viajes",
                                color      = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 17.sp,
                            )
                            Text(
                                text     = "Historial de solicitudes",
                                color    = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                    windowInsets = WindowInsets(0, 0, 0, 0),
                )
            },
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {

                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            when (val state = tripsState) {
                                is UiState.Loading -> {
                                    Box(
                                        modifier         = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            color       = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.5.dp,
                                        )
                                    }
                                }
                                is UiState.Success -> {
                                    if (state.data.isEmpty()) {
                                        EmptyState(
                                            message = "Sin viajes activos",
                                            subtitle = "Solicita un bus desde la pantalla de inicio",
                                            image = Res.drawable.buho_un_poco_triste,
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
                                is UiState.Error -> EmptyState(message = state.message, image = Res.drawable.buho_muy_triste)
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
                        if (onResume != null) {
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = onResume,
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                ),
                            ) {
                                Icon(
                                    Icons.Default.DirectionsBus,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Ver en mapa",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                        Text(
                            text = "← Desliza para cancelar",
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
