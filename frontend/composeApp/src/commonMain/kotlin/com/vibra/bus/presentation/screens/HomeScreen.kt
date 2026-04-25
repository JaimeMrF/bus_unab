package com.vibra.bus.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.data.model.BusSummaryDto
import com.vibra.bus.data.model.StopDto
import com.vibra.bus.presentation.components.BusCard
import com.vibra.bus.presentation.components.EmptyState
import com.vibra.bus.presentation.components.RouteDetailBottomSheet
import com.vibra.bus.presentation.components.ShimmerBusCard
import com.vibra.bus.presentation.components.StopDetailBottomSheet
import com.vibra.bus.presentation.theme.AppColors
import com.vibra.bus.presentation.theme.BottomSheetShape
import com.vibra.bus.presentation.viewmodel.HomeViewModel
import com.vibra.bus.util.UiState
import org.koin.compose.viewmodel.koinViewModel

class HomeScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<HomeViewModel>()
        val busesState by viewModel.busesState.collectAsState()
        val occupancyMap by viewModel.occupancyMap.collectAsState()
        val busStops by viewModel.busStops.collectAsState()
        val busStopsLoading by viewModel.busStopsLoading.collectAsState()
        val snackbarMsg by viewModel.snackbarMessage.collectAsState()
        val userLocation by viewModel.userLocation.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        var selectedBus by remember { mutableStateOf<BusSummaryDto?>(null) }
        var selectedStop by remember { mutableStateOf<StopDto?>(null) }
        var isRefreshing by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            viewModel.startPolling()
        }

        LaunchedEffect(selectedBus) {
            val bus = selectedBus
            if (bus != null) viewModel.loadBusStops(bus.plate)
            else viewModel.clearBusStops()
        }

        DisposableEffect(Unit) {
            onDispose { viewModel.stopPolling() }
        }

        LaunchedEffect(snackbarMsg) {
            snackbarMsg?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.consumeSnackbar()
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { /* center map on user */ },
                    containerColor = AppColors.PrimaryBg,
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Mi ubicación", tint = AppColors.White)
                }
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                MapViewComposable(modifier = Modifier.fillMaxSize(), userLocation = userLocation)

                // Bottom persistent sheet
                ModalBottomSheet(
                    onDismissRequest = {},
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = Color.White,
                    shape = BottomSheetShape,
                    dragHandle = {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth(0.12f)
                                .height(4.dp)
                                .padding(bottom = 4.dp),
                        )
                    },
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "¿A dónde quieres ir?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.DarkText,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(
                            text = "Selecciona tu ruta",
                            fontSize = 13.sp,
                            color = AppColors.GrayText,
                        )
                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                isRefreshing = true
                                viewModel.refresh()
                                isRefreshing = false
                            },
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                        ) {
                            when (val state = busesState) {
                                is UiState.Loading -> {
                                    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                                        items(3) { ShimmerBusCard() }
                                    }
                                }
                                is UiState.Success -> {
                                    if (state.data.isEmpty()) {
                                        EmptyState(
                                            message = "No hay buses activos",
                                            subtitle = "Intenta más tarde",
                                        )
                                    } else {
                                        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                                            items(state.data) { bus ->
                                                BusCard(
                                                    bus = bus,
                                                    occupancy = occupancyMap[bus.plate],
                                                    onClick = { selectedBus = bus },
                                                )
                                            }
                                        }
                                    }
                                }
                                is UiState.Error -> {
                                    EmptyState(
                                        message = "Error cargando buses",
                                        ctaLabel = "Reintentar",
                                        onCtaClick = { viewModel.refresh() },
                                    )
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }

        // Route detail bottom sheet
        selectedBus?.let { bus ->
            RouteDetailBottomSheet(
                bus = bus,
                stops = busStops,
                isLoadingStops = busStopsLoading,
                onDismiss = { selectedBus = null },
                onRequestBus = {
                    selectedBus = null
                    navigator.push(StopSelectionScreen(bus.plate))
                },
            )
        }

        // Stop detail bottom sheet
        selectedStop?.let { stop ->
            StopDetailBottomSheet(
                stop = stop,
                onDismiss = { selectedStop = null },
                onOpenMaps = { /* open maps intent */ },
            )
        }
    }
}
