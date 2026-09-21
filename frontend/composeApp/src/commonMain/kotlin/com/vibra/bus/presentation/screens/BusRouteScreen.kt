package com.vibra.bus.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.data.model.BusSummaryDto
import com.vibra.bus.data.model.StopDto
import com.vibra.bus.data.model.StopWithPivotDto
import com.vibra.bus.presentation.components.EmptyState
import com.vibra.bus.presentation.theme.VibraBusShapes
import com.vibra.bus.presentation.viewmodel.StopSelectionViewModel
import vibrabus.composeapp.generated.resources.Res
import vibrabus.composeapp.generated.resources.leopardo_triste
import vibrabus.composeapp.generated.resources.leopardo_mapa
import com.vibra.bus.util.UiState
import org.koin.compose.viewmodel.koinViewModel

private fun StopWithPivotDto.toStopDto() =
    StopDto(id, name, address, latitude, longitude, radiusMeters)

data class BusRouteScreen(val plate: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator  = LocalNavigator.currentOrThrow
        val viewModel  = koinViewModel<StopSelectionViewModel>()
        val stopsState by viewModel.stopsState.collectAsState()
        val busDetail  by viewModel.busDetail.collectAsState()
        val routePath  by viewModel.routePath.collectAsState()

        LaunchedEffect(Unit) {
            viewModel.loadStops(plate)
            viewModel.loadBusDetail(plate)
        }

        val scaffoldState = rememberBottomSheetScaffoldState()

        val busSummary = busDetail?.let {
            BusSummaryDto(it.id, it.name, it.plate, it.latitude, it.longitude, it.heading)
        }

        val routeStops = (stopsState as? UiState.Success)?.data ?: emptyList()
        val stopDtos   = routeStops.map { it.toStopDto() }

        BottomSheetScaffold(
            scaffoldState       = scaffoldState,
            sheetPeekHeight     = 240.dp,
            sheetContainerColor = Color.Transparent,
            sheetTonalElevation = 0.dp,
            sheetDragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 32.dp, height = 4.dp)
                        .clip(VibraBusShapes.RouteIndicator)
                        .background(MaterialTheme.colorScheme.outline)
                )
            },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                busDetail?.name ?: plate,
                                color      = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (busDetail != null) {
                                Text(
                                    "Placa: $plate",
                                    color    = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
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
                    windowInsets = WindowInsets(0, 0, 0, 0),
                )
            },
            sheetContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(VibraBusShapes.BottomSheet)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), VibraBusShapes.BottomSheet)
                        .padding(16.dp)
                ) {
                    // Bus info card
                    busDetail?.let { bus ->
                        Row(
                            modifier          = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.DirectionsBus,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    bus.name,
                                    color      = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 16.sp
                                )
                                bus.address?.let {
                                    Text(
                                        it,
                                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BusStatChip(
                                icon  = Icons.Default.Speed,
                                label = "${bus.speedKmh} km/h",
                                modifier = Modifier.weight(1f)
                            )
                            BusStatChip(
                                icon  = Icons.Default.Person,
                                label = bus.driver ?: "Sin conductor",
                                modifier = Modifier.weight(1f)
                            )
                            BusStatChip(
                                icon  = Icons.Default.LocationOn,
                                label = "${bus.heading}°",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                    }

                    // Stops list header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Paradas de la ruta",
                            color      = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 15.sp
                        )
                        if (stopsState is UiState.Success) {
                            Text(
                                "${routeStops.size} paradas",
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    AnimatedContent(
                        targetState  = stopsState,
                        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
                        label        = "stops_state",
                    ) { state ->
                    when (state) {
                        is UiState.Loading -> {
                            Box(
                                modifier         = Modifier.fillMaxWidth().height(160.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    color       = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.5.dp,
                                )
                            }
                        }
                        is UiState.Success -> {
                            if (routeStops.isEmpty()) {
                                EmptyState(
                                    message  = "Sin paradas asignadas",
                                    subtitle = "Esta ruta aún no tiene paradas configuradas",
                                    image    = Res.drawable.leopardo_mapa,
                                )
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    itemsIndexed(routeStops) { index, stop ->
                                        RouteStopRow(
                                            stop      = stop,
                                            index     = index,
                                            isLast    = index == routeStops.lastIndex
                                        )
                                    }
                                }
                            }
                        }
                        is UiState.Error -> {
                            EmptyState(
                                message  = "Error al cargar paradas",
                                subtitle = (state as UiState.Error).message,
                                image    = Res.drawable.leopardo_triste,
                            )
                        }
                        else -> {}
                    }
                    } // AnimatedContent

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick  = { navigator.push(StopSelectionScreen(plate)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .navigationBarsPadding(),
                        shape    = VibraBusShapes.ButtonPrimary,
                    ) {
                        Icon(
                            Icons.Default.DirectionsBus,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint     = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Seleccionar parada",
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onPrimary,
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                MapViewComposable(
                    modifier       = Modifier.fillMaxSize(),
                    userLocation   = null,
                    showStops      = true,
                    selectedStop   = null,
                    onStopSelected = {},
                    selectedBus    = busSummary,
                    onBusSelected  = {},
                    stops          = stopDtos,
                    buses          = listOfNotNull(busSummary),
                    path           = routePath.ifEmpty { null }
                )

                if (busDetail == null && stopsState is UiState.Loading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun BusStatChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier          = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun RouteStopRow(
    stop  : StopWithPivotDto,
    index : Int,
    isLast: Boolean,
) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Timeline column
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Numbered circle
            Box(
                modifier         = Modifier
                    .size(28.dp)
                    .background(
                        if (index == 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${index + 1}",
                    color      = Color.White,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                )
            }
        }

        // Stop info
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stop.name,
                    color      = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize   = 14.sp,
                    modifier   = Modifier.weight(1f)
                )
                Text(
                    "~${stop.estimatedMinutes} min",
                    color    = MaterialTheme.colorScheme.secondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                stop.address,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}
