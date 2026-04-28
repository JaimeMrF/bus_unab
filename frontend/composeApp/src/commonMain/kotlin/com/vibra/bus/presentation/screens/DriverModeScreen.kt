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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.vibra.bus.data.model.StopWithPivotDto
import com.vibra.bus.presentation.components.ShimmerBox
import com.vibra.bus.presentation.theme.VibraBusShapes
import com.vibra.bus.presentation.viewmodel.DriverModeViewModel
import com.vibra.bus.presentation.viewmodel.ProfileViewModel
import com.vibra.bus.util.UiState
import org.koin.compose.viewmodel.koinViewModel

// ✅ Shape local — reemplaza el ButtonShape que no existe en M3
private val ButtonShape = RoundedCornerShape(14.dp)

class DriverModeScreen : Screen {

    private val assignedPlate = "RUTA1"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<DriverModeViewModel>()
        val profileViewModel = koinViewModel<ProfileViewModel>()
        val profile by profileViewModel.profile.collectAsState()
        val stopsState by viewModel.stopsState.collectAsState()
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

        val headerScale by animateFloatAsState(
            targetValue = if (isHeaderVisible) 1f else 0.9f,
            animationSpec = tween(durationMillis = 600),
            label = "header_scale"
        )

        LaunchedEffect(Unit) {
            viewModel.loadStops(assignedPlate)
            isHeaderVisible = true
        }
        LaunchedEffect(snackbarMsg) {
            snackbarMsg?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.consumeSnackbar()
            }
        }

        // ✅ Un solo Scaffold
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
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .clip(VibraBusShapes.StatusBadge)
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = pulseAlpha))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = VibraBusShapes.StatusBadge
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "EN RUTA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { navigator.pop() },
                            modifier = Modifier.clip(VibraBusShapes.MapButton)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Regresar",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {

                // ── Header saludo ────────────────────────────────────────────
                item {
                    AnimatedVisibility(
                        visible = isHeaderVisible,
                        enter = slideInVertically(
                            initialOffsetY = { -it },
                            animationSpec = tween(durationMillis = 600)
                        ) + fadeIn(animationSpec = tween(durationMillis = 600))
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
                                text = "Estás asignado a:",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }

                // ── Card ruta asignada ───────────────────────────────────────
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(20.dp),
                                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            )
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        Color(0xFF7A3DB8),
                                    ),
                                ),
                            )
                            .padding(18.dp),
                    ) {
                        Column {
                            Text(
                                text = "Ruta asignada",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f),
                                letterSpacing = 0.8.sp,
                            )
                            Text(
                                text = assignedPlate,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                            Text(
                                text = "Jardín → Casona → CSU",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                                modifier = Modifier.padding(bottom = 14.dp),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = "Bus asignado",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                                    )
                                    Text(
                                        text = "UNAB-01",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary,
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
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha),
                                                    CircleShape,
                                                ),
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "En ruta",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Stats rápidos ────────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        DriverStatCard(value = "12", label = "Pasajeros", modifier = Modifier.weight(1f))
                        DriverStatCard(value = "5",  label = "Paradas",   modifier = Modifier.weight(1f))
                        DriverStatCard(
                            value = "7:00",
                            label = "Próx. salida",
                            valueColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // ── Acciones principales ─────────────────────────────────────
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = { showStopSelector = true },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = ButtonShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                            ),
                            elevation = ButtonDefaults.buttonElevation(4.dp),
                        ) {
                            Text(
                                text = "📍  Confirmar llegada a parada",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            )
                        }

                        OutlinedButton(
                            onClick = { navigator.push(QRScannerScreen()) },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = ButtonShape,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                        ) {
                            Text(
                                text = "📷  Escanear QR de pasajero",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            )
                        }
                    }
                }

                // ── Título paradas ───────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Paradas de la ruta",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (stopsState is UiState.Loading) {
                            Text(
                                text = "Cargando...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // ── Lista de paradas ─────────────────────────────────────────
                when (val state = stopsState) {
                    is UiState.Loading -> {
                        items(5) {
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 5.dp)
                                    .height(80.dp)
                                    .clip(VibraBusShapes.ListItem)
                            )
                        }
                    }
                    is UiState.Success -> {
                        items(state.data) { stop ->
                            DriverStopRow(
                                stop = stop,
                                onConfirm = { viewModel.confirmArrival(assignedPlate, stop.id) },
                            )
                        }
                    }
                    is UiState.Error -> {
                        item {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                        }
                    }
                    else -> {}
                }
            }
        }

        // ── Bottom sheet selector de parada ──────────────────────────────────
        if (showStopSelector && stopsState is UiState.Success) {
            val stops = (stopsState as UiState.Success<List<StopWithPivotDto>>).data
            ModalBottomSheet(
                onDismissRequest = { showStopSelector = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
                containerColor = MaterialTheme.colorScheme.surface,
                shape = VibraBusShapes.BottomSheet,
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "¿En qué parada llegaste?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                    LazyColumn {
                        items(stops) { stop ->
                            StopConfirmRow(
                                stop = stop,
                                onConfirm = {
                                    showStopSelector = false
                                    viewModel.confirmArrival(assignedPlate, stop.id)
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

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun DriverStatCard(
    value: String,
    label: String,
    valueColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(14.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f))
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun DriverStopRow(
    stop: StopWithPivotDto,
    onConfirm: () -> Unit,
) {
    val demandCount = stop.pivot.order
    val (priorityColor, priorityLabel) = when {
        demandCount >= 8 -> Pair(MaterialTheme.colorScheme.error,     "Alta")
        demandCount >= 4 -> Pair(MaterialTheme.colorScheme.secondary, "Media")
        else             -> Pair(MaterialTheme.colorScheme.primary,   "Baja")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .shadow(2.dp, RoundedCornerShape(14.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "${stop.pivot.order}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = stop.name,    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = stop.address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(priorityColor.copy(alpha = 0.10f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(text = priorityLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = priorityColor)
                }
            }

            Spacer(Modifier.width(10.dp))

            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(text = "Confirmar", color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StopConfirmRow(
    stop: StopWithPivotDto,
    onConfirm: () -> Unit,
) {
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
                Text(text = stop.name,    color = MaterialTheme.colorScheme.onSurface,        fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(text = stop.address, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(text = "Confirmar", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}