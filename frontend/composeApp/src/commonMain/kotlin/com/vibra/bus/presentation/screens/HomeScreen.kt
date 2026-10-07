package com.vibra.bus.presentation.screens

import com.vibra.bus.presentation.permissions.rememberPermissionState
import com.vibra.bus.presentation.permissions.PermissionRationaleDialog
import com.vibra.bus.presentation.permissions.AppPermission
import kotlinx.coroutines.delay
import com.vibra.bus.presentation.components.matchesQuery
import com.vibra.bus.presentation.components.SearchField
import com.vibra.bus.data.repository.FavoritesRepository
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.AssistChip
import androidx.compose.foundation.lazy.LazyRow
import com.vibra.bus.presentation.theme.LocalBrand
import com.vibra.bus.presentation.map.busStatesFrom
import com.vibra.bus.domain.brand.MascotPose
import com.vibra.bus.presentation.motion.platformAppActive
import com.vibra.bus.presentation.motion.staggerIn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.data.model.BusSummaryDto
import com.vibra.bus.data.model.StopDto
import com.vibra.bus.presentation.components.BrandMascot
import com.vibra.bus.presentation.components.BusCard
import com.vibra.bus.presentation.components.GlassPanel
import com.vibra.bus.presentation.components.PillTone
import com.vibra.bus.presentation.components.PrimaryButton
import com.vibra.bus.presentation.components.SecondaryButton
import com.vibra.bus.presentation.components.ShimmerList
import com.vibra.bus.presentation.components.StatusPill
import com.vibra.bus.presentation.theme.Motion
import com.vibra.bus.presentation.viewmodel.HomeViewModel
import com.vibra.bus.presentation.viewmodel.ProfileViewModel
import com.vibra.bus.util.AppSettings
import com.vibra.bus.util.UiState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private enum class HomeAction { Driver, Bus, Tracking, Default }

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
        var showBusSheet by remember { mutableStateOf(false) }
        var busQuery by remember { mutableStateOf("") }
        var refreshing by remember { mutableStateOf(false) }
        val favorites = koinInject<FavoritesRepository>()
        val favoriteIds by favorites.ids.collectAsState()
        LaunchedEffect(Unit) { favorites.syncAsync() }
        val haptics = LocalHapticFeedback.current
        val busSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        LaunchedEffect(sessionExpired) {
            if (sessionExpired) navigator.replaceAll(LoginScreen())
        }
        // Polling solo con la app en primer plano y esta pantalla visible.
        val appActive = platformAppActive()
        val locationPermission = rememberPermissionState(AppPermission.Location)
        var showLocationRationale by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            // La explicacion previa se muestra una sola vez, al llegar al mapa (no al abrir la app).
            if (!locationPermission.granted && !settings.locationPromptShown) showLocationRationale = true
        }
        if (showLocationRationale) {
            PermissionRationaleDialog(
                title = "Ver buses cerca de ti",
                message = "Usamos tu ubicación solo mientras usas el mapa, para mostrarte los buses y paradas más cercanos. Puedes seguir sin ella.",
                state = locationPermission,
                onDismiss = {
                    settings.locationPromptShown = true
                    showLocationRationale = false
                },
            )
        }
        // Al conceder el permiso se reinicia el polling para que arranque con la ubicacion.
        LaunchedEffect(appActive, locationPermission.granted) {
            if (appActive) viewModel.startPolling() else viewModel.stopPolling()
        }
        LaunchedEffect(selectedBus) {
            val bus = selectedBus
            if (bus != null) {
                viewModel.loadBusStops(bus.plate)
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
        val busesLoading = busesState is UiState.Loading
        val busesError = busesState is UiState.Error
        val allStops = (stopsState as? UiState.Success)?.data ?: emptyList()
        val routeStops = remember(busStops) {
            busStops.map { s -> StopDto(s.id, s.name, s.address, s.latitude, s.longitude, s.radiusMeters) }
        }
        val stopList = if (selectedBus != null) routeStops else allStops
        val favoriteStops = remember(allStops, favoriteIds) { allStops.filter { it.id in favoriteIds } }
        val filteredBuses = remember(busList, busQuery) {
            busList.filter { matchesQuery(busQuery, it.name, it.plate) }
        }
        LaunchedEffect(refreshing) {
            if (refreshing) { viewModel.refresh(); delay(900); refreshing = false }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0),
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
                    buses = busList,
                    busStates = remember(occupancyMap) { busStatesFrom(occupancyMap) },
                    busStyle = LocalBrand.current.busStyle,
                )

                // ── Panel inferior ────────────────────────────────────────────
                var panelVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { panelVisible = true }
                AnimatedVisibility(
                    visible = panelVisible,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    enter = slideInVertically(tween(Motion.emphasized, easing = Motion.easeOut)) { it } +
                        fadeIn(tween(Motion.emphasized)),
                    exit = fadeOut(tween(Motion.standard)),
                ) {
                    GlassPanel(modifier = Modifier.widthIn(max = 500.dp).fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val firstName = profile.name.split(" ").firstOrNull().orEmpty()
                                    Text(
                                        text = if (firstName.isBlank()) "Hola" else "Hola, $firstName",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = when {
                                            selectedBus != null && busStopsLoading -> "Cargando paradas…"
                                            selectedBus != null && routeStops.isNotEmpty() -> "${routeStops.size} paradas en esta ruta"
                                            selectedBus != null -> "Sin paradas asignadas"
                                            hasTracking -> "Siguiendo ${settings.trackingPlate} · ${settings.trackingStopName}"
                                            busesError -> "No pudimos actualizar los buses"
                                            busesLoading -> "Buscando buses cercanos…"
                                            busList.isNotEmpty() -> "${busList.size} buses activos cerca"
                                            else -> "No hay buses activos por ahora"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (busList.isNotEmpty() && selectedBus == null && !hasTracking) {
                                    StatusPill("En vivo", PillTone.Success, showDot = true)
                                } else if (busesError) {
                                    StatusPill("Sin conexión", PillTone.Error)
                                }
                            }

                            val action = when {
                                profile.role == "driver" -> HomeAction.Driver
                                selectedBus != null -> HomeAction.Bus
                                hasTracking -> HomeAction.Tracking
                                else -> HomeAction.Default
                            }
                            AnimatedContent(
                                targetState = action,
                                transitionSpec = {
                                    (fadeIn(tween(Motion.standard)) + slideInVertically { it / 3 })
                                        .togetherWith(fadeOut(tween(Motion.fast)) + slideOutVertically { -it / 3 })
                                },
                                label = "home_action",
                            ) { state ->
                                when (state) {
                                    HomeAction.Driver -> PrimaryButton(
                                        text = "Entrar a modo conductor",
                                        onClick = { navigator.push(DriverModeScreen()) },
                                    )
                                    HomeAction.Bus -> Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        SecondaryButton(
                                            text = "Ver ruta",
                                            leadingIcon = Icons.Default.Map,
                                            modifier = Modifier.weight(1f),
                                            onClick = { selectedBus?.let { navigator.push(BusRouteScreen(it.plate)) } },
                                        )
                                        PrimaryButton(
                                            text = "Seguir bus",
                                            leadingIcon = Icons.Default.DirectionsBus,
                                            modifier = Modifier.weight(1f),
                                            onClick = { selectedBus?.let { navigator.push(StopSelectionScreen(it.plate)) } },
                                        )
                                    }
                                    HomeAction.Tracking -> Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        SecondaryButton(
                                            text = "Cancelar",
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                settings.clearTracking()
                                                stopBusTracking()
                                            },
                                        )
                                        PrimaryButton(
                                            text = "Ver bus",
                                            leadingIcon = Icons.Default.DirectionsBus,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
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
                                        )
                                    }
                                    HomeAction.Default -> PrimaryButton(
                                        text = "Buscar rutas",
                                        onClick = { showBusSheet = true },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Hoja de rutas ─────────────────────────────────────────────────────
        if (showBusSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBusSheet = false },
                sheetState = busSheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() },
            ) {
                Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Elige tu ruta",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = when {
                                    busesLoading -> "Buscando buses cercanos…"
                                    busList.isEmpty() -> "Sin buses por ahora"
                                    busList.size == 1 -> "1 bus disponible"
                                    else -> "${busList.size} buses disponibles"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (busList.isNotEmpty()) StatusPill("${busList.size}", PillTone.Brand)
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    if (favoriteStops.isNotEmpty()) {
                        Text(
                            "Tus paradas",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 24.dp, top = 12.dp),
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(favoriteStops, key = { it.id }, contentType = { "fav" }) { stop ->
                                AssistChip(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedStop = stop
                                        showBusSheet = false
                                    },
                                    label = { Text(stop.name, maxLines = 1) },
                                    leadingIcon = { Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                )
                            }
                        }
                    }
                    if (busList.size > 4) {
                        SearchField(
                            value = busQuery,
                            onValueChange = { busQuery = it },
                            placeholder = "Buscar ruta o placa",
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    when {
                        busesLoading && busList.isEmpty() -> ShimmerList(count = 3, itemHeight = 72.dp)
                        busList.isEmpty() -> Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            BrandMascot(pose = if (busesError) MascotPose.Sad else MascotPose.Curious, size = 88.dp, neutralFallback = true)
                            Text(
                                text = if (busesError) "No pudimos cargar los buses" else "No hay buses activos ahora",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Intenta de nuevo en unos minutos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        else -> PullToRefreshBox(
                            isRefreshing = refreshing,
                            onRefresh = { refreshing = true },
                        ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 16.dp),
                        ) {
                            itemsIndexed(filteredBuses, key = { _, b -> b.plate }, contentType = { _, _ -> "bus" }) { index, bus ->
                                BusCard(
                                    modifier = Modifier.animateItem().staggerIn(index),
                                    bus = bus,
                                    occupancy = occupancyMap[bus.plate],
                                    onClick = {
                                        scope.launch { busSheetState.hide() }
                                            .invokeOnCompletion {
                                                showBusSheet = false
                                                selectedBus = bus
                                            }
                                    },
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
