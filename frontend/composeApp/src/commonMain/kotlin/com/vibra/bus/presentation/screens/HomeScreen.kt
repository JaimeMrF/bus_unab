package com.vibra.bus.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.data.model.BusSummaryDto
import com.vibra.bus.data.model.StopDto
import androidx.compose.foundation.Image
import com.vibra.bus.presentation.components.BusCard
import com.vibra.bus.presentation.theme.VibraBusShapes
import org.jetbrains.compose.resources.painterResource
import vibrabus.composeapp.generated.resources.Res
import vibrabus.composeapp.generated.resources.leopardo_curioso
import com.vibra.bus.presentation.viewmodel.HomeViewModel
import com.vibra.bus.presentation.viewmodel.ProfileViewModel
import com.vibra.bus.util.AppSettings
import com.vibra.bus.util.UiState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

class HomeScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator        = LocalNavigator.currentOrThrow
        val viewModel        = koinViewModel<HomeViewModel>()
        val profileViewModel = koinViewModel<ProfileViewModel>()
        val settings         = koinInject<AppSettings>()
        val profile          by profileViewModel.profile.collectAsState()
        val hasTracking      by settings.hasActiveTrackingFlow.collectAsState()

        val busesState by viewModel.busesState.collectAsState()
        val stopsState by viewModel.stopsState.collectAsState()
        val busStops by viewModel.busStops.collectAsState()
        val busStopsLoading by viewModel.busStopsLoading.collectAsState()
        val snackbarMsg by viewModel.snackbarMessage.collectAsState()
        val userLocation by viewModel.userLocation.collectAsState()
        val sessionExpired by viewModel.sessionExpired.collectAsState()
        val occupancyMap by viewModel.occupancyMap.collectAsState()

        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        var selectedBus by remember { mutableStateOf<BusSummaryDto?>(null) }
        var selectedStop by remember { mutableStateOf<StopDto?>(null) }
        var showStops by remember { mutableStateOf(false) }
        var isCardVisible by remember { mutableStateOf(false) }
        var showBusSheet by remember { mutableStateOf(false) }
        val busSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        val fabScale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 300),
            label = "fab_scale"
        )

        LaunchedEffect(sessionExpired) {
            if (sessionExpired) navigator.replaceAll(LoginScreen())
        }
        LaunchedEffect(Unit) {
            viewModel.startPolling()
            isCardVisible = true
        }
        LaunchedEffect(selectedBus) {
            if (selectedBus != null) {
                viewModel.loadBusStops(selectedBus!!.plate)
                showStops = true
            } else {
                viewModel.clearBusStops()
                showStops = false
            }
        }
        LaunchedEffect(snackbarMsg) {
            snackbarMsg?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.consumeSnackbar()
            }
        }
        DisposableEffect(Unit) { onDispose { viewModel.stopPolling() } }

        val busList = (busesState as? UiState.Success)?.data ?: emptyList()
        val allStops = (stopsState as? UiState.Success)?.data ?: emptyList()
        val routeStops = busStops.map { s ->
            StopDto(s.id, s.name, s.address, s.latitude, s.longitude, s.radiusMeters)
        }
        val stopList = if (selectedBus != null) routeStops else allStops

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0),
            floatingActionButton = {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { it * 2 }, animationSpec = tween(400))
                            + fadeIn(animationSpec = tween(400))
                ) {
                    FloatingActionButton(
                        onClick = { /* centrar mapa */ },
                        containerColor = MaterialTheme.colorScheme.primary,
                        elevation = FloatingActionButtonDefaults.elevation(8.dp, 12.dp),
                        shape = VibraBusShapes.FloatingActionButton,
                        modifier = Modifier.scale(fabScale)
                    ) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = "Mi ubicación",
                            tint = MaterialTheme.colorScheme.onPrimary
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

                // ── Tarjeta inferior ──────────────────────────────────────────
                AnimatedVisibility(
                    visible = isCardVisible,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(600)
                    ) + fadeIn(animationSpec = tween(600)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 500.dp)
                            .fillMaxWidth()
                            .shadow(
                                elevation = 20.dp,
                                shape = RoundedCornerShape(24.dp),
                                spotColor = Color.Black.copy(alpha = 0.4f)
                            )
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Encabezado
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "¡Hola, ${profile.name.split(" ").firstOrNull() ?: ""}! 🦉",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 22.sp
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = when {
                                            selectedBus != null && busStopsLoading -> "Cargando paradas..."
                                            selectedBus != null && routeStops.isNotEmpty() -> "${routeStops.size} paradas en esta ruta"
                                            selectedBus != null -> "Sin paradas asignadas"
                                            hasTracking -> "Siguiendo ${settings.trackingPlate} · ${settings.trackingStopName}"
                                            busList.isNotEmpty() -> "${busList.size} buses activos cerca"
                                            else -> "Buscando buses cercanos..."
                                        },
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Indicador live cuando hay buses (no mostrar si hay tracking activo)
                                if (busList.isNotEmpty() && selectedBus == null && !hasTracking) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFF2EBE6C).copy(alpha = 0.15f))
                                            .border(1.dp, Color(0xFF2EBE6C).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color(0xFF2EBE6C), CircleShape)
                                        )
                                        Spacer(Modifier.size(5.dp))
                                        Text(
                                            text = "En vivo",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF2EBE6C)
                                        )
                                    }
                                }
                            }

                            // Acciones — AnimatedContent hace slide entre los 4 estados
                            val actionState = when {
                                profile.role == "driver" -> "driver"
                                selectedBus != null      -> "bus"
                                hasTracking              -> "tracking"
                                else                     -> "default"
                            }
                            AnimatedContent(
                                targetState  = actionState,
                                transitionSpec = {
                                    (fadeIn(tween(220)) + slideInVertically { it / 3 })
                                        .togetherWith(fadeOut(tween(150)) + slideOutVertically { -it / 3 })
                                },
                                label = "home_action",
                            ) { state ->
                                when (state) {
                                    "driver" -> Button(
                                        onClick  = { navigator.push(DriverModeScreen()) },
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                        shape    = VibraBusShapes.ButtonPrimary,
                                    ) {
                                        Text("Entrar a Modo Conductor", fontWeight = FontWeight.SemiBold)
                                    }
                                    "bus" -> Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        OutlinedButton(
                                            onClick  = { navigator.push(BusRouteScreen(selectedBus!!.plate)) },
                                            modifier = Modifier.weight(1f).height(50.dp),
                                            shape    = VibraBusShapes.ButtonPrimary,
                                            border   = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                        ) {
                                            Icon(Icons.Default.Map, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(6.dp))
                                            Text("Ver Ruta", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                        }
                                        Button(
                                            onClick  = { navigator.push(StopSelectionScreen(selectedBus!!.plate)) },
                                            modifier = Modifier.weight(1f).height(50.dp),
                                            shape    = VibraBusShapes.ButtonPrimary,
                                        ) {
                                            Icon(Icons.Default.DirectionsBus, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                            Spacer(Modifier.width(6.dp))
                                            Text("Seguir Bus", fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    "tracking" -> Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        OutlinedButton(
                                            onClick  = {
                                                settings.clearTracking()
                                                stopBusTracking()
                                            },
                                            modifier = Modifier.weight(1f).height(50.dp),
                                            shape    = VibraBusShapes.ButtonPrimary,
                                            border   = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                        ) {
                                            Text("Cancelar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                                        }
                                        Button(
                                            onClick  = {
                                                val stop = StopDto(
                                                    id           = settings.trackingStopId,
                                                    name         = settings.trackingStopName,
                                                    address      = settings.trackingStopAddress,
                                                    latitude     = settings.trackingStopLat,
                                                    longitude    = settings.trackingStopLng,
                                                    radiusMeters = 50,
                                                )
                                                startBusTracking(settings.trackingPlate, stop.latitude, stop.longitude, stop.name)
                                                navigator.push(WaitingBusScreen(settings.trackingPlate, stop))
                                            },
                                            modifier = Modifier.weight(1f).height(50.dp),
                                            shape    = VibraBusShapes.ButtonPrimary,
                                        ) {
                                            Icon(Icons.Default.DirectionsBus, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                            Spacer(Modifier.width(6.dp))
                                            Text("Ver bus", fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    else -> Button(
                                        onClick  = { showBusSheet = true },
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                        shape    = VibraBusShapes.ButtonPrimary,
                                    ) {
                                        Text("Buscar Rutas", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Bottom Sheet estilo Uber ──────────────────────────────────────────
        if (showBusSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBusSheet = false },
                sheetState = busSheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(top = 14.dp, bottom = 6.dp)
                            .size(width = 36.dp, height = 4.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    // Cabecera
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Elige tu ruta",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = when {
                                    busList.isEmpty() -> "Buscando buses cercanos..."
                                    busList.size == 1 -> "1 bus disponible"
                                    else -> "${busList.size} buses disponibles"
                                },
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Badge con conteo
                        if (busList.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${busList.size}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                    Spacer(Modifier.height(8.dp))

                    if (busList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    painter = painterResource(Res.drawable.leopardo_curioso),
                                    contentDescription = null,
                                    modifier = Modifier.size(90.dp),
                                )
                                Text(
                                    text = "No hay buses activos ahora",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Intenta de nuevo en unos minutos",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 12.dp),
                        ) {
                            items(busList, key = { it.plate }) { bus ->
                                BusCard(
                                    modifier = Modifier.animateItem(fadeInSpec = tween(300), fadeOutSpec = tween(200)),
                                    bus = bus,
                                    occupancy = occupancyMap[bus.plate],
                                    onClick = {
                                        scope.launch { busSheetState.hide() }
                                            .invokeOnCompletion {
                                                showBusSheet = false
                                                selectedBus = bus
                                            }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
