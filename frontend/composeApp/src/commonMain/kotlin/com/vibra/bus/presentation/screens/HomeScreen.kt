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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.blur
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
import com.vibra.bus.presentation.theme.vibraBusColors
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
        
        val occupancyMap by viewModel.occupancyMap.collectAsState()
        val busStops by viewModel.busStops.collectAsState()
        val busStopsLoading by viewModel.busStopsLoading.collectAsState()
        val snackbarMsg by viewModel.snackbarMessage.collectAsState()
        val userLocation by viewModel.userLocation.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

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
        val stopList = (stopsState as? UiState.Success)?.data ?: emptyList()

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
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(durationMillis = 600)
                    ) + fadeIn(animationSpec = tween(durationMillis = 600)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 300))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(VibraBusShapes.BottomSheet)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.vibraBusColors.glassSurface,
                                        MaterialTheme.vibraBusColors.glassBorder
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.vibraBusColors.glassHighlight,
                                        MaterialTheme.vibraBusColors.glassBorder
                                    )
                                ),
                                shape = VibraBusShapes.BottomSheet
                            )
                            .shadow(
                                elevation = 12.dp,
                                shape = VibraBusShapes.BottomSheet,
                                spotColor = Color.Black.copy(alpha = 0.15f)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 20.dp)
                        ) {
                            // Drag Handle Indicator
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 16.dp)
                                    .size(width = 40.dp, height = 4.dp)
                                    .clip(VibraBusShapes.RouteIndicator)
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.secondary,
                                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                                            )
                                        )
                                    )
                            )

                            AnimatedVisibility(
                                visible = true,
                                enter = slideInVertically(
                                    initialOffsetY = { -it / 2 },
                                    animationSpec = tween(durationMillis = 800, delayMillis = 200)
                                ) + fadeIn(
                                    animationSpec = tween(durationMillis = 800, delayMillis = 200)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "¡Hola, ${
                                                profile.name.split(" ").firstOrNull() ?: ""
                                            }! 🦉",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = if (selectedBus != null) 
                                                "${selectedBus?.name ?: "Bus UNAB"}" 
                                            else if (activeBusCount > 0)
                                                "$activeBusCount buses activos cerca"
                                            else
                                                "Selecciona tu ruta",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            
                            // Espacio para la lista de rutas o botón
                            if (profile.role == "driver") {
                                com.vibra.bus.presentation.components.PrimaryGlassButton(
                                    text = "Entrar a Modo Conductor",
                                    onClick = { navigator.push(DriverModeScreen()) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                com.vibra.bus.presentation.components.PrimaryGlassButton(
                                    text = if (selectedBus != null) "Ver ruta ${selectedBus?.plate}" else "Selecciona un bus",
                                    onClick = { 
                                        selectedBus?.let { 
                                            navigator.push(StopSelectionScreen(it.plate)) 
                                        }
                                    },
                                    enabled = selectedBus != null,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
