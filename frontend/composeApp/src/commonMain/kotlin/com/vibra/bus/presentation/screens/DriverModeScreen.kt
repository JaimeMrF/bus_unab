package com.vibra.bus.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.data.model.StopWithPivotDto
import com.vibra.bus.presentation.theme.AppColors
import com.vibra.bus.presentation.theme.BottomSheetShape
import com.vibra.bus.presentation.theme.ButtonShape
import com.vibra.bus.presentation.viewmodel.DriverModeViewModel
import com.vibra.bus.presentation.viewmodel.ProfileViewModel
import com.vibra.bus.util.UiState
import org.koin.compose.viewmodel.koinViewModel

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

        LaunchedEffect(Unit) { viewModel.loadStops(assignedPlate) }

        LaunchedEffect(snackbarMsg) {
            snackbarMsg?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.consumeSnackbar()
            }
        }

        if (showStopSelector && stopsState is UiState.Success) {
            val stops = (stopsState as UiState.Success<List<StopWithPivotDto>>).data
            ModalBottomSheet(
                onDismissRequest = { showStopSelector = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
                containerColor = AppColors.DarkHeader,
                shape = BottomSheetShape,
            ) {
                Text(
                    "Seleccionar parada de llegada",
                    fontWeight = FontWeight.Bold,
                    color = AppColors.White,
                    modifier = Modifier.padding(16.dp),
                )
                LazyColumn {
                    items(stops) { stop ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = AppColors.PrimaryBg),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stop.name, color = AppColors.White, fontWeight = FontWeight.Medium)
                                    Text(stop.address, color = AppColors.GrayText, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {
                                        showStopSelector = false
                                        viewModel.confirmArrival(assignedPlate, stop.id)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.GreenSuccess),
                                    shape = RoundedCornerShape(8.dp),
                                ) { Text("Confirmar", color = AppColors.White) }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Modo Conductor", color = AppColors.White) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, "Volver", tint = AppColors.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.PrimaryBg),
                )
            },
            containerColor = AppColors.PrimaryBg,
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "¡Hola, ${profile.name}!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.White,
                )
                Text(
                    text = "Estás asignado a:",
                    fontSize = 14.sp,
                    color = AppColors.GrayText,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.DarkHeader),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Ruta asignada", color = AppColors.GrayText, fontSize = 12.sp)
                        Text(assignedPlate, color = AppColors.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(AppColors.GreenActive, CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text("En ruta", color = AppColors.GreenActive, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { showStopSelector = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = ButtonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.AccentOrange),
                ) {
                    Text("Confirmar llegada a parada", color = AppColors.White, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { navigator.push(QRScannerScreen()) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = ButtonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryPurple),
                ) {
                    Text("Escanear QR de pasajero", color = AppColors.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
