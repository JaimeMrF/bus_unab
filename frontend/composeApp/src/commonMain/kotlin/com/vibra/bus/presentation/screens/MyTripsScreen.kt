package com.vibra.bus.presentation.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import com.vibra.bus.data.model.RequestInfo
import com.vibra.bus.presentation.components.EmptyState
import com.vibra.bus.presentation.theme.AppColors
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
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(snackbarMsg) {
            snackbarMsg?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.consumeSnackbar()
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = AppColors.PrimaryBg,
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Text(
                    text = "Mis Viajes",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                )
                PullToRefreshBox(
                    isRefreshing = false,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when (val state = tripsState) {
                        is UiState.Loading -> {}
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripCard(trip: RequestInfo, onCancel: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart && trip.status == "pending") {
                onCancel()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.Red, RoundedCornerShape(12.dp))
                    .padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("Cancelar", color = AppColors.White, fontWeight = FontWeight.SemiBold)
            }
        },
        modifier = Modifier.padding(vertical = 6.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.DarkHeader),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.CardActive),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row {
                    Text(
                        text = trip.bus.name,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.White,
                        modifier = Modifier.weight(1f),
                    )
                    StatusChip(status = trip.status)
                }
                Spacer(Modifier.height(6.dp))
                Text("Parada: ${trip.stop.name}", color = AppColors.GrayText, fontSize = 13.sp)
                Text(trip.stop.address, color = AppColors.GrayText, fontSize = 12.sp)
                if (trip.status == "pending") {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Desliza para cancelar →",
                        fontSize = 11.sp,
                        color = AppColors.GrayText.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (bg, label) = when (status) {
        "pending" -> Pair(AppColors.AccentOrange.copy(alpha = 0.2f), "Pendiente")
        "active" -> Pair(AppColors.GreenActive.copy(alpha = 0.2f), "Activo")
        "completed" -> Pair(AppColors.GreenSuccess.copy(alpha = 0.2f), "Completado")
        "cancelled" -> Pair(AppColors.Red.copy(alpha = 0.2f), "Cancelado")
        else -> Pair(Color.Gray.copy(alpha = 0.2f), status)
    }
    Box(
        modifier = Modifier.background(bg, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, color = AppColors.White, fontSize = 12.sp)
    }
}
