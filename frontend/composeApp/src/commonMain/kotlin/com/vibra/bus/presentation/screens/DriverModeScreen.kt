package com.vibra.bus.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.data.model.BusCatalogItem
import com.vibra.bus.data.model.StopWithPivotDto
import com.vibra.bus.presentation.components.ShimmerBox
import com.vibra.bus.presentation.theme.VibraBusShapes
import com.vibra.bus.presentation.viewmodel.DriverModeViewModel
import com.vibra.bus.presentation.viewmodel.ProfileViewModel
import com.vibra.bus.util.UiState
import org.koin.compose.viewmodel.koinViewModel

private val ButtonShape = RoundedCornerShape(14.dp)

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
        val snackbarMsg by viewModel.snackbarMessage.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        var showStopSelector by remember { mutableStateOf(false) }
        var isHeaderVisible by remember { mutableStateOf(false) }

        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulseAlpha",
        )

        LaunchedEffect(Unit) { isHeaderVisible = true }
        LaunchedEffect(snackbarMsg) {
            snackbarMsg?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.consumeSnackbar()
            }
        }

        // Selector de bus si no hay ruta activa
        if (activePlate.isEmpty()) {
            BusSelectorSheet(
                catalogState = catalogState,
                driverName = profile.name.split(" ").firstOrNull() ?: profile.name,
                onSelect = { viewModel.selectBus(it) },
                onBack = { if (canPop) navigator.pop() },
                showBack = canPop,
            )
            return
        }

        val stopCount = (stopsState as? UiState.Success)?.data?.size ?: 0

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Modo Conductor",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .clip(VibraBusShapes.StatusBadge)
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = pulseAlpha))
                                    .border(1.dp, MaterialTheme.colorScheme.secondary, VibraBusShapes.StatusBadge)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = "EN RUTA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (canPop) {
                            IconButton(onClick = { navigator.pop() }, modifier = Modifier.clip(VibraBusShapes.MapButton)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.changeBus() }) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Cambiar ruta", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.surface)))
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {

                // ── Header saludo ─────────────────────────────────────────────
                item {
                    AnimatedVisibility(
                        visible = isHeaderVisible,
                        enter = slideInVertically(initialOffsetY = { -it }, animationSpec = tween(600)) + fadeIn(tween(600)),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                        ) {
                            Text(
                                text = "¡Hola, ${profile.name.split(" ").firstOrNull() ?: profile.name}!",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Text(
                                text = "Estás conduciendo:",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }

                // ── Card ruta asignada ────────────────────────────────────────
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF7A3DB8))))
                            .padding(18.dp),
                    ) {
                        Column {
                            Text(
                                text = "Ruta activa",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f),
                                letterSpacing = 0.8.sp,
                            )
                            Text(
                                text = activePlate,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TextButton(
                                    onClick = { viewModel.changeBus() },
                                    contentPadding = PaddingValues(horizontal = 0.dp),
                                ) {
                                    Text(
                                        text = "Cambiar ruta →",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f))
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha), CircleShape),
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(text = "En ruta", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Stats reales ──────────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        DriverStatCard(value = passengerCount.toString(), label = "Pasajeros", modifier = Modifier.weight(1f))
                        DriverStatCard(value = stopCount.toString(), label = "Paradas", modifier = Modifier.weight(1f))
                        DriverStatCard(
                            value = if (isFull) "LLENO" else "Libre",
                            label = "Estado",
                            valueColor = if (isFull) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // ── Acciones principales ──────────────────────────────────────
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = { showStopSelector = true },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = ButtonShape,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            elevation = ButtonDefaults.buttonElevation(4.dp),
                        ) {
                            Text("📍  Confirmar llegada a parada", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        OutlinedButton(
                            onClick = { navigator.push(QRScannerScreen()) },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = ButtonShape,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                        ) {
                            Text("📷  Escanear QR de pasajero", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }

                // ── Toggle ocupación ──────────────────────────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape)
                                        .background(if (isFull) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = if (isFull) Icons.Default.Groups else Icons.Default.Group,
                                        contentDescription = null,
                                        tint = if (isFull) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Estado del Bus", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = if (isFull) "Bus LLENO (Sin cupos)" else "Hay asientos disponibles",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isFull) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Switch(
                                checked = isFull,
                                onCheckedChange = { viewModel.toggleOccupancy(activePlate) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.error,
                                    checkedTrackColor = MaterialTheme.colorScheme.errorContainer,
                                ),
                            )
                        }
                    }
                }

                // ── Título paradas ────────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Paradas de la ruta", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        if (stopsState is UiState.Loading) {
                            Text("Cargando...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // ── Lista de paradas ──────────────────────────────────────────
                when (val state = stopsState) {
                    is UiState.Loading -> {
                        items(5) {
                            ShimmerBox(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).height(80.dp).clip(VibraBusShapes.ListItem))
                        }
                    }
                    is UiState.Success -> {
                        items(state.data) { stop ->
                            DriverStopRow(
                                stop = stop,
                                onApproaching = { viewModel.notifyApproaching(activePlate, stop.id) },
                                onConfirm = { viewModel.confirmArrival(activePlate, stop.id) },
                            )
                        }
                    }
                    is UiState.Error -> {
                        item {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp))
                        }
                    }
                    else -> {}
                }
            }
        }

        // ── Bottom sheet selector de parada ───────────────────────────────────
        if (showStopSelector && stopsState is UiState.Success) {
            val stops = (stopsState as UiState.Success<List<StopWithPivotDto>>).data
            ModalBottomSheet(
                onDismissRequest = { showStopSelector = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
                containerColor = MaterialTheme.colorScheme.surface,
                shape = VibraBusShapes.BottomSheet,
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text("¿En qué parada llegaste?", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 12.dp))
                    LazyColumn {
                        items(stops) { stop ->
                            StopConfirmRow(stop = stop, onConfirm = { showStopSelector = false; viewModel.confirmArrival(activePlate, stop.id) })
                        }
                        item { Spacer(Modifier.height(32.dp)) }
                    }
                }
            }
        }
    }
}

// ── Selector de bus (pantalla completa cuando no hay ruta activa) ─────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BusSelectorSheet(
    catalogState: UiState<List<BusCatalogItem>>,
    driverName: String,
    onSelect: (BusCatalogItem) -> Unit,
    onBack: () -> Unit,
    showBack: Boolean = true,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Selecciona tu ruta", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Hola, $driverName", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("¿Qué ruta estás conduciendo hoy?", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))

            when (val state = catalogState) {
                is UiState.Loading -> {
                    repeat(3) {
                        ShimmerBox(modifier = Modifier.fillMaxWidth().height(80.dp).padding(vertical = 6.dp).clip(RoundedCornerShape(14.dp)))
                    }
                }
                is UiState.Success -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(state.data) { bus ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { onSelect(bus) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(2.dp),
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(bus.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text(bus.plate, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Capacidad: ${bus.capacity} pasajeros", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
                is UiState.Error -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
                else -> {}
            }
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun DriverStatCard(value: String, label: String, valueColor: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(14.dp), ambientColor = Color.Black.copy(alpha = 0.1f))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.05f))), RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = if (valueColor == MaterialTheme.colorScheme.primary) Color.White else valueColor)
            Text(text = label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun DriverStopRow(stop: StopWithPivotDto, onApproaching: () -> Unit, onConfirm: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .shadow(2.dp, RoundedCornerShape(14.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${stop.pivot?.order ?: stop.order}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stop.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(stop.address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onApproaching,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(vertical = 6.dp),
                ) {
                    Text("Aproximando", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(vertical = 6.dp),
                ) {
                    Text("Llegué", color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StopConfirmRow(stop: StopWithPivotDto, onConfirm: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .shadow(2.dp, RoundedCornerShape(12.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stop.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(stop.address, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text("Confirmar", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
