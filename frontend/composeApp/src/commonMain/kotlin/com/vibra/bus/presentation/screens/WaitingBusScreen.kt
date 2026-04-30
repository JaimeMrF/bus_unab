package com.vibra.bus.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.data.model.StopDto
import com.vibra.bus.presentation.components.PrimaryGlassButton
import com.vibra.bus.presentation.theme.VibraBusShapes
import com.vibra.bus.presentation.viewmodel.WaitingBusViewModel
import org.koin.compose.viewmodel.koinViewModel

data class WaitingBusScreen(val plate: String, val stop: StopDto) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<WaitingBusViewModel>()
        val bus by viewModel.bus.collectAsState()
        val eta by viewModel.etaMinutes.collectAsState()
        val distance by viewModel.distanceMeters.collectAsState()
        val isArriving by viewModel.isArriving.collectAsState()
        var isVisible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            viewModel.startTracking(plate, stop)
            isVisible = true
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Siguiendo bus", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, "Volver")
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
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Background Map
                MapViewComposable(
                    modifier = Modifier.fillMaxSize(),
                    userLocation = null, // In real app, pass current location
                    showStops = true,
                    selectedStop = stop,
                    onStopSelected = {},
                    selectedBus = bus,
                    onBusSelected = {},
                    stops = listOf(stop),
                    buses = bus?.let { listOf(it) } ?: emptyList(),
                    path = if (bus != null) listOf(
                        com.vibra.bus.util.LatLng(bus!!.latitude, bus!!.longitude),
                        com.vibra.bus.util.LatLng(stop.latitude, stop.longitude)
                    ) else null
                )

                // Overlay Notification when arriving
                AnimatedVisibility(
                    visible = isArriving,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.NotificationsActive, null, tint = Color.White)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "¡Tu bus está llegando!",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Bottom Info Card
                AnimatedVisibility(
                    visible = isVisible,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(VibraBusShapes.BottomSheet)
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = bus?.name ?: "Buscando bus...",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Placa: $plate",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // ETA Circle
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = eta?.toString() ?: "--",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "min",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        
                        LinearProgressIndicator(
                            progress = { 
                                // Simulate progress based on distance (closer = more progress)
                                val d = distance ?: 1000
                                (1f - (d.toFloat() / 1000f)).coerceIn(0.1f, 1f)
                            },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                        
                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = if (isArriving) "¡Prepara tu QR para abordar!" else "Aproximadamente a ${(distance ?: 0) / 100} cuadras",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        PrimaryGlassButton(
                            text = "Mostrar mi QR",
                            onClick = { navigator.push(MyQRScreen()) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
}

