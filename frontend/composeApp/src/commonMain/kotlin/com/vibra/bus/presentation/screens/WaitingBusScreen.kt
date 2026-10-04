package com.vibra.bus.presentation.screens

import com.vibra.bus.domain.brand.MascotPose
import com.vibra.bus.presentation.components.CelebrationOverlay
import com.vibra.bus.presentation.motion.PulseRings
import androidx.compose.foundation.layout.width
import com.vibra.bus.presentation.theme.Motion
import com.vibra.bus.presentation.components.PrimaryButton
import com.vibra.bus.presentation.components.AppTopBar
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import com.vibra.bus.presentation.theme.LocalBrand
import com.vibra.bus.presentation.components.BrandMascot
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.data.model.StopDto
import com.vibra.bus.presentation.theme.AppShape
import com.vibra.bus.presentation.viewmodel.WaitingBusViewModel
import com.vibra.bus.util.AppSettings
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

expect fun startBusTracking(plate: String, stopLat: Double, stopLng: Double, stopName: String)
expect fun stopBusTracking()
expect fun isIgnoringBatteryOptimizations(): Boolean
expect fun openBatteryOptimizationSettings()

data class WaitingBusScreen(val plate: String, val stop: StopDto) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel  = koinViewModel<WaitingBusViewModel>()
        val settings   = koinInject<AppSettings>()
        val bus        by viewModel.bus.collectAsState()
        val eta        by viewModel.etaMinutes.collectAsState()
        val distance   by viewModel.distanceMeters.collectAsState()
        val isArriving by viewModel.isArriving.collectAsState()
        val routePath  by viewModel.routePath.collectAsState()
        var isVisible        by remember { mutableStateOf(false) }
        var showBatteryDialog by remember { mutableStateOf(false) }

        var arrivals by remember { mutableStateOf(0) }
        LaunchedEffect(isArriving) { if (isArriving) arrivals++ }

        LaunchedEffect(Unit) {
            settings.saveTracking(plate, stop.id, stop.name, stop.address, stop.latitude, stop.longitude)
            viewModel.startTracking(plate, stop)
            startBusTracking(plate, stop.latitude, stop.longitude, stop.name)
            isVisible = true
            if (!settings.batteryPromptShown && !isIgnoringBatteryOptimizations()) {
                showBatteryDialog = true
            }
        }

        if (showBatteryDialog) {
            AlertDialog(
                onDismissRequest = {
                    settings.batteryPromptShown = true
                    showBatteryDialog = false
                },
                icon = { BrandMascot(pose = MascotPose.Phone, size = 72.dp) },
                title = { Text("Mantén el seguimiento activo") },
                text = {
                    Text(
                        "Para que la notificación del bus siga visible aunque cierres la app, " +
                        "necesitamos que desactives la optimización de batería para ${LocalBrand.current.appName}. " +
                        "Toca \"Activar\" y selecciona \"No restringir\"."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        settings.batteryPromptShown = true
                        showBatteryDialog = false
                        openBatteryOptimizationSettings()
                    }) { Text("Activar") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        settings.batteryPromptShown = true
                        showBatteryDialog = false
                    }) { Text("Ahora no") }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = AppShape.Dialog,
            )
        }

        val cancelTracking = {
            settings.clearTracking()
            stopBusTracking()
            navigator.pop()
        }

        Scaffold(
            topBar = { AppTopBar(
                title = "Siguiendo bus",
                subtitle = bus?.name ?: plate,
                onBack = { cancelTracking () },
                windowInsets = TopAppBarDefaults.windowInsets,
            ) }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                MapViewComposable(
                    modifier = Modifier.fillMaxSize(),
                    userLocation = null,
                    showStops = true,
                    selectedStop = stop,
                    onStopSelected = {},
                    selectedBus = bus,
                    onBusSelected = {},
                    stops = listOf(stop),
                    buses = bus?.let { listOf(it) } ?: emptyList(),
                    path = routePath.ifEmpty {
                        bus?.let {
                            listOf(
                                com.vibra.bus.util.LatLng(it.latitude, it.longitude),
                                com.vibra.bus.util.LatLng(stop.latitude, stop.longitude),
                            )
                        }
                    },
                )

                CelebrationOverlay(trigger = arrivals, pose = MascotPose.Celebrating)

                // Aviso de llegada: superficie de acento + icono + texto (no solo color)
                AnimatedVisibility(
                    visible = isArriving,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                ) {
                    Surface(
                        shape = AppShape.Card,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 8.dp,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "¡Tu bus está llegando!",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isVisible,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically(tween(Motion.emphasized, easing = Motion.easeOut)) { it } + fadeIn(),
                    exit = fadeOut(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AppShape.BottomSheet)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, AppShape.BottomSheet)
                            .navigationBarsPadding()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bus?.name ?: "Buscando bus…",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Placa $plate",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Box(
                                modifier = Modifier.size(96.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                            if (isArriving || bus == null) PulseRings(Modifier.fillMaxSize())
                            if (bus == null) BrandMascot(pose = MascotPose.Waiting, size = 56.dp)
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .semantics(mergeDescendants = true) {
                                        contentDescription = eta?.let { "Llega en $it minutos" } ?: "Calculando tiempo de llegada"
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = eta?.toString() ?: "–",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                    Text(
                                        text = "min",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                            }
                        }

                        val progress by animateFloatAsState(
                            targetValue = (1f - ((distance ?: 1000).toFloat() / 1000f)).coerceIn(0.1f, 1f),
                            animationSpec = tween(Motion.emphasized),
                            label = "eta_progress",
                        )
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer,
                        )

                        Text(
                            text = if (isArriving) "¡Prepara tu QR para abordar!"
                            else "Aproximadamente a ${(distance ?: 0) / 100} cuadras",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PrimaryButton(
                                text = "Mostrar mi QR",
                                leadingIcon = Icons.Default.QrCode,
                                onClick = {
                                    settings.clearTracking()
                                    stopBusTracking()
                                    navigator.push(MyQRScreen())
                                },
                            )
                            TextButton(
                                onClick = { cancelTracking () },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            ) {
                                Text(
                                    "Cancelar seguimiento",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
