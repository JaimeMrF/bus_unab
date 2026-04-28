package com.vibra.bus.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.data.model.StopDto
import com.vibra.bus.data.model.StopWithPivotDto
import com.vibra.bus.presentation.components.EmptyState
import com.vibra.bus.presentation.components.ShimmerBox
import com.vibra.bus.presentation.theme.VibraBusShapes
import com.vibra.bus.presentation.viewmodel.StopSelectionViewModel
import com.vibra.bus.util.UiState
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

// ✅ Local utility to convert models for the map
private fun StopWithPivotDto.toStopDto() = StopDto(id, name, address, latitude, longitude, radiusMeters)

data class StopSelectionScreen(val plate: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator         = LocalNavigator.currentOrThrow
        val viewModel         = koinViewModel<StopSelectionViewModel>()
        val stopsState        by viewModel.stopsState.collectAsState()
        val selectedStop      by viewModel.selectedStop.collectAsState()
        val requestState      by viewModel.requestState.collectAsState()
        val busDetail         by viewModel.busDetail.collectAsState()
        val snackbarMsg       by viewModel.snackbarMessage.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        var showFullDialog    by remember { mutableStateOf(false) }
        val listState         = rememberLazyListState()
        val bottomSheetState  = rememberBottomSheetScaffoldState()
        val scope             = rememberCoroutineScope()

        val sheetScale by animateFloatAsState(
            targetValue    = 1f,
            animationSpec  = tween(durationMillis = 400),
            label          = "sheet_scale"
        )

        LaunchedEffect(Unit) {
            viewModel.loadStops(plate)
            viewModel.loadBusDetail(plate)
        }
        LaunchedEffect(snackbarMsg) {
            snackbarMsg?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.consumeSnackbar()
            }
        }
        LaunchedEffect(requestState) {
            when (val state = requestState) {
                is UiState.Success -> {
                    if (state.data.isFull) showFullDialog = true
                    else {
                        viewModel.consumeRequestState()
                        navigator.push(MyQRScreen())
                    }
                }
                else -> {}
            }
        }

        BottomSheetScaffold(
            scaffoldState = bottomSheetState,
            snackbarHost  = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Seleccionar parada",
                            color      = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            sheetContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .scale(sheetScale)
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text  = "Selecciona tu parada",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text  = "Bus: $plate",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(VibraBusShapes.StatusBadge)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text  = if (selectedStop != null) "Seleccionado" else "Pendiente",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    val stopList = (stopsState as? UiState.Success)?.data ?: emptyList()

                    when (val state = stopsState) {
                        is UiState.Loading -> {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(5) {
                                    ShimmerBox(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(70.dp)
                                            .clip(VibraBusShapes.ListItem)
                                    )
                                }
                            }
                        }
                        is UiState.Success -> {
                            if (state.data.isEmpty()) {
                                EmptyState(
                                    message = "Sin paradas disponibles",
                                    subtitle = "No hay paradas registradas para esta ruta"
                                )
                            } else {
                                LazyColumn(
                                    state = listState,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(state.data) { stop ->
                                        StopSelectionRow(
                                            stop       = stop,
                                            isSelected = selectedStop?.id == stop.id,
                                            onClick    = { viewModel.selectStop(stop) }
                                        )
                                    }
                                }
                            }
                        }
                        is UiState.Error -> {
                            EmptyState(
                                message = "Error al cargar paradas",
                                subtitle = state.message
                            )
                        }
                        else -> {}
                    }

                    Spacer(Modifier.height(16.dp))

                    busDetail?.let { bus ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation    = 3.dp,
                                    shape        = RoundedCornerShape(14.dp),
                                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                )
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp),
                        ) {
                            Column {
                                Text(
                                    bus.name,
                                    fontWeight = FontWeight.Bold,
                                    color      = MaterialTheme.colorScheme.onSurface,
                                    fontSize   = 16.sp
                                )
                                Text(
                                    "Placa: ${bus.plate}",
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        listOf("Ruta", "Parada", "Confirmar", "QR").forEachIndexed { i, label ->
                            val active = i == 1
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            if (active) MaterialTheme.colorScheme.primary
                                            else        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                            RoundedCornerShape(14.dp)
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "${i + 1}",
                                        color      = MaterialTheme.colorScheme.onPrimary,
                                        fontSize   = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    label,
                                    color    = if (active) MaterialTheme.colorScheme.primary
                                    else        MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    val isLoading = requestState is UiState.Loading
                    Button(
                        onClick  = {
                            val stop = selectedStop ?: return@Button
                            val bus  = busDetail    ?: return@Button
                            viewModel.confirmStop(bus.id, stop.id)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape    = VibraBusShapes.ButtonPrimary,
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                        ),
                        enabled  = selectedStop != null && !isLoading,
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(20.dp),
                                color       = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Confirmar parada",
                                color      = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            },
            sheetPeekHeight      = 120.dp,
            sheetDragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 32.dp, height = 4.dp)
                        .clip(VibraBusShapes.RouteIndicator)
                        .background(MaterialTheme.colorScheme.outline)
                )
            },
            sheetContainerColor  = MaterialTheme.colorScheme.surfaceContainer,
            sheetTonalElevation  = 8.dp,
            containerColor       = MaterialTheme.colorScheme.background
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val stopList = (stopsState as? UiState.Success)?.data ?: emptyList()
                val stopDtos = stopList.map { it.toStopDto() }

                MapViewComposable(
                    modifier        = Modifier.fillMaxSize(),
                    userLocation    = null,
                    showStops       = true,
                    selectedStop    = selectedStop?.toStopDto(),
                    onStopSelected  = { stopDto ->
                        val fullStop = stopList.find { it.id == stopDto.id }
                        if (fullStop != null) {
                            viewModel.selectStop(fullStop)
                            scope.launch {
                                bottomSheetState.bottomSheetState.partialExpand()
                            }
                        }
                    },
                    stops = stopDtos,
                    buses = emptyList()
                )

                AnimatedVisibility(
                    visible  = selectedStop != null,
                    enter    = slideInVertically(
                        initialOffsetY = { it * 2 },
                        animationSpec  = tween(durationMillis = 300)
                    ) + fadeIn(animationSpec = tween(durationMillis = 300)),
                    exit     = fadeOut(animationSpec = tween(durationMillis = 200)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 140.dp)
                ) {
                    Button(
                        onClick  = {
                            val stop = selectedStop ?: return@Button
                            val bus  = busDetail    ?: return@Button
                            viewModel.confirmStop(bus.id, stop.id)
                        },
                        enabled  = selectedStop != null && requestState !is UiState.Loading,
                        shape    = VibraBusShapes.ButtonPrimary,
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor   = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Confirmar")
                    }
                }
            }
        }

        if (showFullDialog) {
            AlertDialog(
                onDismissRequest = { showFullDialog = false },
                title = {
                    Text(
                        "Bus lleno",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        "El bus seleccionado está lleno. ¿Deseas continuar de todas formas?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showFullDialog = false
                        viewModel.consumeRequestState()
                        navigator.push(MyQRScreen())
                    }) {
                        Text("Continuar", color = MaterialTheme.colorScheme.primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFullDialog = false }) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
            )
        }
    }
}

@Composable
private fun StopSelectionRow(
    stop       : StopWithPivotDto,
    isSelected : Boolean,
    onClick    : () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .shadow(
                elevation    = 2.dp,
                shape        = RoundedCornerShape(12.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            )
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
                else            MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = isSelected,
                onClick  = onClick,
                colors   = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                ),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    stop.name,
                    color      = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    stop.address,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Text(
                "~${stop.pivot.estimatedMinutes} min",
                color      = MaterialTheme.colorScheme.primary,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
