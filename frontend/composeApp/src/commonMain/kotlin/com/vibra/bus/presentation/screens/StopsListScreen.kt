package com.vibra.bus.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.vibra.bus.presentation.viewmodel.StopSelectionViewModel
import com.vibra.bus.util.UiState
import org.koin.compose.viewmodel.koinViewModel

data class StopsListScreen(val plate: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<StopSelectionViewModel>()
        val stopsState by viewModel.stopsState.collectAsState()
        val busDetail by viewModel.busDetail.collectAsState()

        LaunchedEffect(Unit) {
            viewModel.loadStops(plate)
            viewModel.loadBusDetail(plate)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Paradas - ${busDetail?.name ?: plate}",
                            color = AppColors.White,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, "Volver", tint = AppColors.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.PrimaryPurple),
                )
            },
            containerColor = AppColors.PrimaryBg,
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (val state = stopsState) {
                    is UiState.Loading -> {
                        repeat(5) { ShimmerBox(height = 70.dp) }
                    }
                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            EmptyState(message = "Sin paradas disponibles")
                        } else {
                            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                                itemsIndexed(state.data) { index, stop ->
                                    StopListItem(stop = stop, isFirst = index == 0, isLast = index == state.data.lastIndex)
                                }
                            }
                        }
                    }
                    is UiState.Error -> EmptyState(message = state.message)
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun StopListItem(stop: StopWithPivotDto, isFirst: Boolean, isLast: Boolean) {
    val dotColor = when {
        isFirst -> AppColors.PrimaryPurple
        isLast  -> AppColors.GreenSuccess
        else    -> AppColors.PrimaryPurple.copy(alpha = 0.55f)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = dotColor),
            ) {
                Text(
                    text = "${stop.pivot.order}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Bold,
                    color = AppColors.White,
                    fontSize = 13.sp,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stop.name,    color = AppColors.TextPrimary,   fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(stop.address, color = AppColors.TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text("~${stop.pivot.estimatedMinutes} min", color = AppColors.AccentOrange, fontSize = 12.sp)
            }
        }
    }
}
