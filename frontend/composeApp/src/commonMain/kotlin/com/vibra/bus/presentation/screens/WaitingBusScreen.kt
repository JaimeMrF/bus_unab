package com.vibra.bus.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import com.vibra.bus.data.model.StopDto
import com.vibra.bus.presentation.components.PrimaryGlassButton
import com.vibra.bus.presentation.theme.VibraBusShapes
import com.vibra.bus.presentation.viewmodel.WaitingBusViewModel
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import vibrabus.composeapp.generated.resources.Res
import vibrabus.composeapp.generated.resources.buho_viendo_mapa

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
        val routePath by viewModel.routePath.collectAsState()
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
                    path = routePath.ifEmpty { 
                        if (bus != null) listOf(
                            com.vibra.bus.util.LatLng(bus!!.latitude, bus!!.longitude),
                            com.vibra.bus.util.LatLng(stop.latitude, stop.longitude)
                        ) else null
                    }
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
                                        Color(0xB31D1B31), // Dark Glass
                                        Color(0xE61D1B31)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(listOf(Color(0x33FFFFFF), Color(0x1AFFFFFF))),
                                shape = VibraBusShapes.BottomSheet
                            )
                            .padding(20.dp)
                    ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bus?.name ?: "Buscando bus...",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Placa: $plate",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            if (bus == null) {
                                Image(
                                    painter = painterResource(Res.drawable.buho_viendo_mapa),
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                            
                            // ETA Circle
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = eta?.toString() ?: "--",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSecondary
                                    )
                                    Text(
                                        text = "min",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f)
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
                            color = Color.White.copy(alpha = 0.7f),
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

