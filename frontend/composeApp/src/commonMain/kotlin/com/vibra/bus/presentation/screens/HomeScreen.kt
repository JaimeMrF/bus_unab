package com.vibra.bus.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.data.model.BusSummaryDto
import com.vibra.bus.data.model.StopDto
import com.vibra.bus.presentation.theme.VibraBusShapes
import com.vibra.bus.presentation.viewmodel.HomeViewModel
import com.vibra.bus.presentation.viewmodel.ProfileViewModel
import com.vibra.bus.util.UiState
import org.koin.compose.viewmodel.koinViewModel

class HomeScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<HomeViewModel>()
        val profileViewModel = koinViewModel<ProfileViewModel>()
        val profile by profileViewModel.profile.collectAsState()
        
        val busesState by viewModel.busesState.collectAsState()
        val stopsState by viewModel.stopsState.collectAsState()
        val busStops by viewModel.busStops.collectAsState()
        val busStopsLoading by viewModel.busStopsLoading.collectAsState()
        
        val snackbarMsg by viewModel.snackbarMessage.collectAsState()
        val userLocation by viewModel.userLocation.collectAsState()
        val sessionExpired by viewModel.sessionExpired.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        LaunchedEffect(sessionExpired) {
            if (sessionExpired) {
                navigator.replaceAll(LoginScreen())
            }
        }

        var selectedBus by remember { mutableStateOf<BusSummaryDto?>(null) }
        var selectedStop by remember { mutableStateOf<StopDto?>(null) }
        var showStops by remember { mutableStateOf(true) }
        var isSheetVisible by remember { mutableStateOf(false) }

        val fabScale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 300),
            label = "fab_scale"
        )

        LaunchedEffect(Unit) {
            viewModel.startPolling()
            isSheetVisible = true
        }

        LaunchedEffect(selectedBus) {
            val bus = selectedBus
            if (bus != null) viewModel.loadBusStops(bus.plate)
            else viewModel.clearBusStops()
        }

        DisposableEffect(Unit) { onDispose { viewModel.stopPolling() } }

        LaunchedEffect(snackbarMsg) {
            snackbarMsg?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.consumeSnackbar()
            }
        }

        val activeBusCount = if (busesState is UiState.Success)
            (busesState as UiState.Success).data.size else 0

        val busList = (busesState as? UiState.Success)?.data ?: emptyList()
        val allStops = (stopsState as? UiState.Success)?.data ?: emptyList()
        val routeStops = busStops.map { s ->
            StopDto(s.id, s.name, s.address, s.latitude, s.longitude, s.radiusMeters)
        }
        val stopList = if (selectedBus != null) routeStops else allStops

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { it * 2 },
                        animationSpec = tween(durationMillis = 400)
                    ) + fadeIn(animationSpec = tween(durationMillis = 400))
                ) {
                    FloatingActionButton(
                        onClick = { /* centrar mapa en usuario */ },
                        containerColor = MaterialTheme.colorScheme.primary,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 12.dp
                        ),
                        shape = VibraBusShapes.FloatingActionButton,
                        modifier = Modifier.scale(fabScale)
                    ) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = "Mi ubicación",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {

                MapViewComposable(
                    modifier = Modifier.fillMaxSize(),
                    userLocation = userLocation,
                    showStops = showStops,
                    selectedStop = selectedStop,
                    onStopSelected = { selectedStop = it },
                    selectedBus = selectedBus,
                    onBusSelected = { selectedBus = it },
                    stops = stopList,
                    buses = busList
                )

                AnimatedVisibility(
                    visible = isSheetVisible,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(durationMillis = 600)
                    ) + fadeIn(animationSpec = tween(durationMillis = 600)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 300))
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 500.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xB31D1B31),
                                        Color(0xE61D1B31)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0x33FFFFFF), Color(0x1AFFFFFF))
                                ),
                                shape = RoundedCornerShape(28.dp)
                            )
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(28.dp),
                                spotColor = Color.Black.copy(alpha = 0.4f)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "¡Hola, ${profile.name.split(" ").firstOrNull() ?: ""}! 🦉",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = when {
                                            selectedBus != null && busStopsLoading -> "Cargando paradas..."
                                            selectedBus != null && routeStops.isNotEmpty() -> "${routeStops.size} paradas en ruta"
                                            selectedBus != null -> "Sin paradas asignadas"
                                            activeBusCount > 0 -> "$activeBusCount buses cerca"
                                            else -> "Selecciona una parada"
                                        },
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            
                            if (profile.role == "driver") {
                                com.vibra.bus.presentation.components.PrimaryGlassButton(
                                    text = "Entrar a Modo Conductor",
                                    onClick = { navigator.push(DriverModeScreen()) },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                )
                            } else {
                                if (selectedBus != null) {
                                    androidx.compose.foundation.layout.Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        com.vibra.bus.presentation.components.PrimaryGlassButton(
                                            text = "Ver Ruta",
                                            onClick = { navigator.push(BusRouteScreen(selectedBus!!.plate)) },
                                            modifier = Modifier.weight(1f).height(48.dp)
                                        )
                                        com.vibra.bus.presentation.components.PrimaryGlassButton(
                                            text = "Seguir Bus",
                                            onClick = { navigator.push(StopSelectionScreen(selectedBus!!.plate)) },
                                            modifier = Modifier.weight(1f).height(48.dp)
                                        )
                                    }
                                } else {
                                    com.vibra.bus.presentation.components.PrimaryGlassButton(
                                        text = "Buscar Rutas",
                                        onClick = {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Toca un bus en el mapa para seleccionarlo")
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
