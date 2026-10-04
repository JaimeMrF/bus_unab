package com.vibra.bus.presentation.screens

import org.koin.compose.koinInject
import com.vibra.bus.presentation.components.matchesQuery
import com.vibra.bus.presentation.components.SearchField
import com.vibra.bus.presentation.components.FavoriteHeart
import com.vibra.bus.data.repository.FavoritesRepository
import com.vibra.bus.domain.brand.MascotPose
import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.BorderStroke
import com.vibra.bus.presentation.components.StatusPill
import com.vibra.bus.presentation.components.ShimmerList
import com.vibra.bus.presentation.components.PrimaryButton
import com.vibra.bus.presentation.components.PillTone
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.material3.BottomSheetDefaults
import com.vibra.bus.presentation.components.AppTopBar
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.data.model.StopDto
import com.vibra.bus.data.model.StopWithPivotDto
import com.vibra.bus.presentation.components.EmptyState
import com.vibra.bus.presentation.theme.AppShape
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
        val favorites         = koinInject<FavoritesRepository>()
        val favoriteIds by favorites.ids.collectAsState()
        var query by remember { mutableStateOf("") }

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
                AppTopBar(
                    title = busDetail?.name ?: "Seleccionar parada",
                    subtitle = "Elige dónde subirte",
                    onBack = { navigator.pop() },
                )
            },
            sheetContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShape.BottomSheet)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, AppShape.BottomSheet)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text  = "¿Dónde subes?",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.semantics { heading() },
                        )
                        if (selectedStop != null) {
                            StatusPill("Parada elegida", PillTone.Success, showDot = true)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Lista de paradas con altura máxima — nunca empuja el botón fuera
                    when (val state = stopsState) {
                        is UiState.Loading -> {
                            ShimmerList(count = 3, itemHeight = 64.dp)
                        }
                        is UiState.Success -> {
                            if (state.data.isEmpty()) {
                                EmptyState(
                                    message = "Sin paradas disponibles",
                                    subtitle = "No hay paradas registradas para esta ruta"
                                )
                            } else {
                                if (state.data.size > 6) {
                                    SearchField(query, { query = it }, "Buscar parada")
                                    Spacer(Modifier.height(8.dp))
                                }
                                // Favoritas primero; el filtro es instantaneo y se recalcula solo si cambian sus entradas.
                                val visible = remember(state.data, query, favoriteIds) {
                                    state.data
                                        .filter { matchesQuery(query, it.name, it.address) }
                                        .sortedByDescending { it.id in favoriteIds }
                                }
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.heightIn(max = 280.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(visible, key = { it.id }, contentType = { "stop" }) { stop ->
                                        StopSelectionRow(
                                            stop       = stop,
                                            isSelected = selectedStop?.id == stop.id,
                                            favorite   = stop.id in favoriteIds,
                                            onToggleFavorite = { favorites.toggle(stop.id) },
                                            onClick    = { viewModel.selectStop(stop) }
                                        )
                                    }
                                }
                            }
                        }
                        is UiState.Error -> {
                            EmptyState(
                    pose = MascotPose.Sad,
                                message = "No pudimos cargar las paradas",
                                subtitle = state.message,
                                ctaLabel = "Reintentar",
                                onCtaClick = { viewModel.loadStops(plate) },
                            )
                        }
                        else -> {}
                    }

                    Spacer(Modifier.height(16.dp))

                    // Botón siempre visible al fondo del sheet
                    val isLoading = requestState is UiState.Loading
                    PrimaryButton(
                        text = selectedStop?.let { "Confirmar: ${it.name}" } ?: "Elige una parada",
                        loading = isLoading,
                        enabled = selectedStop != null,
                        onClick = {
                            selectedStop?.let { stop ->
                                busDetail?.let { bus -> viewModel.confirmStop(bus.id, stop.id) }
                            }
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            },
            sheetPeekHeight      = 380.dp,
            sheetDragHandle = { BottomSheetDefaults.DragHandle() },
            sheetContainerColor  = Color.Transparent,
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
                        Text("Continuar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFullDialog = false }) {
                        Text("Cancelar")
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
    favorite   : Boolean,
    onToggleFavorite: () -> Unit,
    onClick    : () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick),
        shape = AppShape.ListItem,
        color = if (isSelected) colors.primaryContainer else colors.surfaceVariant,
        border = if (isSelected) BorderStroke(1.5.dp, colors.primary) else null,
    ) {
        Row(
            modifier = Modifier.heightIn(min = 56.dp).padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = isSelected, onClick = null, modifier = Modifier.padding(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stop.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) colors.onPrimaryContainer else colors.onSurface,
                )
                Text(
                    text = stop.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                )
            }
            Text(
                text = if (stop.estimatedMinutes == 0) "Salida" else "~${stop.estimatedMinutes} min",
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) colors.onPrimaryContainer else colors.primary,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            FavoriteHeart(favorite = favorite, onToggle = onToggleFavorite)
        }
    }
}
