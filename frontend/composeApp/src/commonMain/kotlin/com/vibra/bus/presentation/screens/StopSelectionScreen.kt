package com.vibra.bus.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
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
import vibrabus.composeapp.generated.resources.Res
import vibrabus.composeapp.generated.resources.buho_muy_triste
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
        val routePath         by viewModel.routePath.collectAsState()
        val snackbarMsg       by viewModel.snackbarMessage.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        var showFullDialog    by remember { mutableStateOf(false) }
        val listState         = rememberLazyListState()
        val bottomSheetState  = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberStandardBottomSheetState(
                initialValue = SheetValue.Expanded,
                skipHiddenState = true
            )
        )
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
                        selectedStop?.let { stop ->
                            navigator.push(WaitingBusScreen(plate, stop.toStopDto()))
                        }
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
                        Column {
                            Text(
                                text       = busDetail?.name ?: "Seleccionar parada",
                                color      = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 17.sp
                            )
                            Text(
                                text     = "Elige dónde subirte",
                                color    = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
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
                    ),
                    windowInsets = WindowInsets(0, 0, 0, 0)
                )
            },
            sheetContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(VibraBusShapes.BottomSheet)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xB31D1B31), // Dark Glass
                                    Color(0xE61D1B31)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0x33FFFFFF), Color(0x1AFFFFFF))
                            ),
                            shape = VibraBusShapes.BottomSheet
                        )
                        .padding(16.dp)
                        .scale(sheetScale)
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text       = "¿Dónde subes?",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 18.sp,
                            color      = Color.White
                        )
                        if (selectedStop != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF4CAF50).copy(alpha = 0.2f))
                                    .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text      = "✓ Seleccionada",
                                    fontSize  = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color     = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Lista de paradas con altura máxima — nunca empuja el botón fuera
                    when (val state = stopsState) {
                        is UiState.Loading -> {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 280.dp),
                                contentPadding = PaddingValues(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(4) {
                                    ShimmerBox(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(64.dp)
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
                                    modifier = Modifier.heightIn(max = 280.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                subtitle = state.message,
                                image = Res.drawable.buho_muy_triste,
                            )
                        }
                        else -> {}
                    }

                    Spacer(Modifier.height(16.dp))

                    // Botón siempre visible al fondo del sheet
                    val isLoading = requestState is UiState.Loading
                    Button(
                        onClick = {
                            selectedStop?.let { stop ->
                                busDetail?.let { bus ->
                                    viewModel.confirmStop(bus.id, stop.id)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape  = VibraBusShapes.ButtonPrimary,
                        colors = ButtonDefaults.buttonColors(
                            containerColor         = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                        ),
                        enabled = selectedStop != null && !isLoading,
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(20.dp),
                                color       = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                if (selectedStop != null) "Confirmar — ${selectedStop!!.name}"
                                else "Elige una parada",
                                color      = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 15.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            },
            sheetPeekHeight      = 460.dp,
            sheetDragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 32.dp, height = 4.dp)
                        .clip(VibraBusShapes.RouteIndicator)
                        .background(MaterialTheme.colorScheme.outline)
                )
            },
            sheetContainerColor  = Color.Transparent, // Transparent to use our custom glass background
            sheetTonalElevation  = 0.dp,
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
                    selectedBus     = null,
                    onBusSelected   = {},
                    stops = stopDtos,
                    buses = emptyList(),
                    path = routePath
                )

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
                if (isSelected) Color.White.copy(alpha = 0.15f)
                else            Color.White.copy(alpha = 0.05f)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = isSelected,
                onClick  = onClick,
                colors   = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.secondary,
                    unselectedColor = Color.White.copy(alpha = 0.6f)
                ),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    stop.name,
                    color      = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    stop.address,
                    color    = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
            Text(
                "~${stop.estimatedMinutes} min",
                color      = MaterialTheme.colorScheme.secondary,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
