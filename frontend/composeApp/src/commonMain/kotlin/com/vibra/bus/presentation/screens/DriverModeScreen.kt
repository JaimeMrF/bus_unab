package com.vibra.bus.presentation.screens

import com.vibra.bus.presentation.permissions.rememberPermissionState
import com.vibra.bus.presentation.permissions.PermissionRationaleDialog
import com.vibra.bus.presentation.permissions.AppPermission
import com.vibra.bus.domain.brand.MascotPose
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.data.model.BusCatalogItem
import com.vibra.bus.data.model.StopWithPivotDto
import com.vibra.bus.presentation.components.AppCard
import com.vibra.bus.presentation.components.AppTopBar
import com.vibra.bus.presentation.components.BrandMascot
import com.vibra.bus.presentation.components.EmptyState
import com.vibra.bus.presentation.components.PillTone
import com.vibra.bus.presentation.components.PrimaryButton
import com.vibra.bus.presentation.components.SecondaryButton
import com.vibra.bus.presentation.components.ShimmerList
import com.vibra.bus.presentation.components.StatusPill
import com.vibra.bus.presentation.theme.AppShape
import com.vibra.bus.presentation.viewmodel.DriverModeViewModel
import com.vibra.bus.presentation.viewmodel.LocationSource
import com.vibra.bus.presentation.viewmodel.ProfileViewModel
import com.vibra.bus.util.UiState
import org.koin.compose.viewmodel.koinViewModel

class DriverModeScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val canPop = navigator.canPop
        val viewModel = koinViewModel<DriverModeViewModel>()
        val profileViewModel = koinViewModel<ProfileViewModel>()
        val profile by profileViewModel.profile.collectAsState()
        val stopsState by viewModel.stopsState.collectAsState()
        val catalogState by viewModel.catalogState.collectAsState()
        val activePlate by viewModel.activePlate.collectAsState()
        val isFull by viewModel.isFull.collectAsState()
        val passengerCount by viewModel.passengerCount.collectAsState()
        val locationSource by viewModel.locationSource.collectAsState()
        val snackbarMsg by viewModel.snackbarMessage.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        var showStopSelector by remember { mutableStateOf(false) }
        val phoneLocation = rememberPermissionState(AppPermission.Location)
        var showPhoneRationale by remember { mutableStateOf(false) }
        if (showPhoneRationale) {
            PermissionRationaleDialog(
                title = "Enviar la ubicación del bus",
                message = "Para usar tu teléfono como GPS del bus necesitamos tu ubicación mientras conduces. Se envía solo en modo conductor.",
                state = phoneLocation,
                onDismiss = { showPhoneRationale = false },
            )
        }

        LaunchedEffect(snackbarMsg) {
            snackbarMsg?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.consumeSnackbar()
            }
        }

        // Selector de bus si no hay ruta activa
        if (activePlate.isEmpty()) {
            BusSelectorScreen(
                catalogState = catalogState,
                driverName = profile.name.split(" ").firstOrNull() ?: profile.name,
                onSelect = { viewModel.selectBus(it) },
                onBack = if (canPop) ({ navigator.pop() }) else null,
            )
            return
        }

        val stopCount = (stopsState as? UiState.Success)?.data?.size ?: 0

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                AppTopBar(
                    title = "Modo conductor",
                    onBack = if (canPop) ({ navigator.pop() }) else null,
                    actions = {
                        IconButton(onClick = { viewModel.changeBus() }) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = "Cambiar ruta",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // ── Ruta activa ──────────────────────────────────────────────
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShape.CardLarge,
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Hola, ${profile.name.split(" ").firstOrNull() ?: profile.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Text(
                                    text = "Ruta activa",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Text(
                                    text = activePlate,
                                    style = MaterialTheme.typography.displayMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                            BrandMascot(pose = MascotPose.Driver, size = 64.dp)
                        }
                    }
                }

                // ── Métricas ─────────────────────────────────────────────────
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DriverStatCard(passengerCount.toString(), "Pasajeros", Modifier.weight(1f))
                        DriverStatCard(stopCount.toString(), "Paradas", Modifier.weight(1f))
                        DriverStatCard(
                            value = if (isFull) "Lleno" else "Libre",
                            label = "Estado",
                            modifier = Modifier.weight(1f),
                            emphasized = isFull,
                        )
                    }
                }

                // ── Acciones ─────────────────────────────────────────────────
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryButton(
                            text = "Confirmar llegada a parada",
                            leadingIcon = Icons.Default.PinDrop,
                            onClick = { showStopSelector = true },
                        )
                        SecondaryButton(
                            text = "Escanear QR de pasajero",
                            leadingIcon = Icons.Default.CameraAlt,
                            onClick = { navigator.push(QRScannerScreen()) },
                        )
                    }
                }

                // ── Ocupación ────────────────────────────────────────────────
                item {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            if (isFull) MaterialTheme.colorScheme.errorContainer
                                            else MaterialTheme.colorScheme.primaryContainer,
                                            CircleShape,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = if (isFull) Icons.Default.Groups else Icons.Default.Group,
                                        contentDescription = null,
                                        tint = if (isFull) MaterialTheme.colorScheme.onErrorContainer
                                        else MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Estado del bus",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = if (isFull) "Bus lleno (sin cupos)" else "Hay asientos disponibles",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Switch(
                                checked = isFull,
                                onCheckedChange = { viewModel.toggleOccupancy(activePlate) },
                                modifier = Modifier.semantics { contentDescription = "Bus lleno" },
                            )
                        }
                    }
                }

                // ── Fuente de ubicación ──────────────────────────────────────
                item {
                    LocationSourceCard(
                        current = locationSource,
                        onChange = {
                            if (it == LocationSource.PHONE_GPS && !phoneLocation.granted) showPhoneRationale = true
                            viewModel.setLocationSource(it)
                        },
                    )
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            "Paradas de la ruta",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f).semantics { heading() },
                        )
                    }
                }

                when (val state = stopsState) {
                    is UiState.Loading -> item { ShimmerList(count = 3, itemHeight = 100.dp) }
                    is UiState.Success -> items(state.data, key = { it.id }) { stop ->
                        DriverStopRow(
                            stop = stop,
                            onApproaching = { viewModel.notifyApproaching(activePlate, stop.id) },
                            onConfirm = { viewModel.confirmArrival(activePlate, stop.id) },
                        )
                    }
                    is UiState.Error -> item {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    else -> {}
                }
            }
        }

        // ── Hoja: ¿en qué parada llegaste? ───────────────────────────────────
        if (showStopSelector && stopsState is UiState.Success) {
            val stops = (stopsState as UiState.Success<List<StopWithPivotDto>>).data
            ModalBottomSheet(
                onDismissRequest = { showStopSelector = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
                containerColor = MaterialTheme.colorScheme.surface,
                shape = AppShape.BottomSheet,
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        "¿En qué parada llegaste?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 12.dp).semantics { heading() },
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(stops, key = { it.id }) { stop ->
                            StopConfirmRow(
                                stop = stop,
                                onConfirm = {
                                    showStopSelector = false
                                    viewModel.confirmArrival(activePlate, stop.id)
                                },
                            )
                        }
                        item { Spacer(Modifier.height(32.dp)) }
                    }
                }
            }
        }
    }
}

// ── Selector de bus (pantalla completa cuando no hay ruta activa) ─────────────

@Composable
private fun BusSelectorScreen(
    catalogState: UiState<List<BusCatalogItem>>,
    driverName: String,
    onSelect: (BusCatalogItem) -> Unit,
    onBack: (() -> Unit)?,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { AppTopBar(title = "Selecciona tu ruta", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Hola, $driverName",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        "¿Qué ruta estás conduciendo hoy?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BrandMascot(pose = MascotPose.Driver, size = 64.dp)
            }
            Spacer(Modifier.height(12.dp))

            when (val state = catalogState) {
                is UiState.Loading -> ShimmerList(count = 4, itemHeight = 80.dp)
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        EmptyState(
                            message = "No hay rutas disponibles",
                            subtitle = "Pide a tu organización que te asigne un bus",
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 24.dp),
                        ) {
                            items(state.data, key = { it.plate }) { bus ->
                                AppCard(modifier = Modifier.fillMaxWidth(), onClick = { onSelect(bus) }) {
                                    Row(
                                        modifier = Modifier.heightIn(min = 72.dp).padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                Icons.Default.DirectionsBus,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            )
                                        }
                                        Spacer(Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(bus.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                            Text(bus.plate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                "Capacidad: ${bus.capacity} pasajeros",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is UiState.Error -> EmptyState(
                    pose = MascotPose.Sad,
                    message = "No pudimos cargar las rutas",
                    subtitle = state.message,
                )
                else -> {}
            }
        }
    }
}

// ── Fuente de ubicación ───────────────────────────────────────────────────────

@Composable
private fun LocationSourceCard(
    current: LocationSource,
    onChange: (LocationSource) -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Fuente de ubicación del bus",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LocationSourceChip(
                    label = "GPS del bus",
                    icon = Icons.Default.DirectionsBus,
                    selected = current == LocationSource.BUS_GPS,
                    onClick = { onChange(LocationSource.BUS_GPS) },
                    modifier = Modifier.weight(1f),
                )
                LocationSourceChip(
                    label = "Mi teléfono",
                    icon = Icons.Default.MyLocation,
                    selected = current == LocationSource.PHONE_GPS,
                    onClick = { onChange(LocationSource.PHONE_GPS) },
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = if (current == LocationSource.PHONE_GPS)
                    "Tu ubicación se envía al servidor cada pocos segundos"
                else
                    "Se usa el rastreador GPS instalado en el vehículo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LocationSourceChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = AppShape.Chip,
        color = if (selected) scheme.primaryContainer else scheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
            )
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun DriverStatCard(value: String, label: String, modifier: Modifier = Modifier, emphasized: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = AppShape.CardSmall,
        color = if (emphasized) scheme.errorContainer else scheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = if (emphasized) scheme.onErrorContainer else scheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (emphasized) scheme.onErrorContainer else scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DriverStopRow(stop: StopWithPivotDto, onApproaching: () -> Unit, onConfirm: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(34.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${stop.pivot?.order ?: stop.order}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stop.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(stop.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(text = "Aproximando", onClick = onApproaching, modifier = Modifier.weight(1f))
                PrimaryButton(text = "Llegué", onClick = onConfirm, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StopConfirmRow(stop: StopWithPivotDto, onConfirm: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onConfirm) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stop.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(stop.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusPill(text = "Confirmar", tone = PillTone.Brand)
        }
    }
}
