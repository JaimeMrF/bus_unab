package com.vibra.bus.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.data.model.StopWithPivotDto
import com.vibra.bus.presentation.components.EmptyState
import com.vibra.bus.presentation.components.ShimmerBox
import com.vibra.bus.presentation.theme.AppColors
import com.vibra.bus.presentation.theme.ButtonShape
import com.vibra.bus.presentation.viewmodel.StopSelectionViewModel
import com.vibra.bus.util.UiState
import org.koin.compose.viewmodel.koinViewModel

data class StopSelectionScreen(val plate: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<StopSelectionViewModel>()
        val stopsState by viewModel.stopsState.collectAsState()
        val selectedStop by viewModel.selectedStop.collectAsState()
        val requestState by viewModel.requestState.collectAsState()
        val busDetail by viewModel.busDetail.collectAsState()
        val snackbarMsg by viewModel.snackbarMessage.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        var showFullDialog by remember { mutableStateOf(false) }

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
                    if (state.data.isFull) {
                        showFullDialog = true
                    } else {
                        viewModel.consumeRequestState()
                        navigator.push(MyQRScreen())
                    }
                }
                else -> {}
            }
        }

        if (showFullDialog) {
            AlertDialog(
                onDismissRequest = { showFullDialog = false },
                title = { Text("Bus lleno") },
                text = { Text("El bus seleccionado está lleno. ¿Deseas continuar de todas formas?") },
                confirmButton = {
                    TextButton(onClick = {
                        showFullDialog = false
                        viewModel.consumeRequestState()
                        navigator.push(MyQRScreen())
                    }) { Text("Continuar") }
                },
                dismissButton = {
                    TextButton(onClick = { showFullDialog = false }) { Text("Cancelar") }
                },
            )
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Seleccionar parada", color = AppColors.White) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Volver", tint = AppColors.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.PrimaryBg),
                )
            },
            containerColor = AppColors.PrimaryBg,
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            ) {
                // Route info card
                busDetail?.let { bus ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.DarkHeader),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(bus.name, fontWeight = FontWeight.Bold, color = AppColors.White, fontSize = 16.sp)
                            Text("Placa: ${bus.plate}", color = AppColors.GrayText, fontSize = 13.sp)
                        }
                    }
                }

                // Progress indicator
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    listOf("Ruta", "Parada", "Confirmar", "QR").forEachIndexed { i, label ->
                        val active = i == 1
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        if (active) AppColors.AccentOrange else AppColors.MediumPurple,
                                        RoundedCornerShape(14.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${i + 1}", color = AppColors.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(label, color = if (active) AppColors.AccentOrange else AppColors.GrayText, fontSize = 10.sp)
                        }
                    }
                }

                Text(
                    text = "Toca una parada para seleccionarla",
                    fontSize = 12.sp,
                    color = AppColors.GrayText,
                    modifier = Modifier.padding(vertical = 8.dp),
                )

                when (val state = stopsState) {
                    is UiState.Loading -> {
                        repeat(4) { ShimmerBox(height = 70.dp) }
                    }
                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            EmptyState(
                                message = "No hay paradas para esta ruta",
                                subtitle = "El conductor aún no ha registrado paradas",
                            )
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(state.data) { stop ->
                                    StopSelectionRow(
                                        stop = stop,
                                        isSelected = selectedStop?.id == stop.id,
                                        onClick = { viewModel.selectStop(stop) },
                                    )
                                }
                            }
                        }
                    }
                    is UiState.Error -> {
                        EmptyState(message = state.message)
                    }
                    else -> {}
                }

                val isLoading = requestState is UiState.Loading
                Button(
                    onClick = {
                        val stop = selectedStop ?: return@Button
                        val bus = busDetail ?: return@Button
                        viewModel.confirmStop(bus.id, stop.id)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp).padding(vertical = 4.dp),
                    shape = ButtonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryBg),
                    enabled = selectedStop != null && !isLoading,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AppColors.White, strokeWidth = 2.dp)
                    } else {
                        Text("Confirmar parada", color = AppColors.White, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun StopSelectionRow(
    stop: StopWithPivotDto,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AppColors.MediumPurple.copy(alpha = 0.3f) else AppColors.DarkHeader,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = AppColors.AccentOrange),
            )
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(stop.name, color = AppColors.White, fontWeight = FontWeight.Medium)
                Text(stop.address, color = AppColors.GrayText, fontSize = 12.sp)
            }
            Text(
                "~${stop.pivot.estimatedMinutes} min",
                color = AppColors.AccentOrange,
                fontSize = 12.sp,
            )
        }
    }
}
