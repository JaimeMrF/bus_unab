package com.vibra.bus.presentation.screens

import com.vibra.bus.domain.brand.MascotPose
import com.vibra.bus.presentation.motion.staggerIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.data.model.StopWithPivotDto
import com.vibra.bus.presentation.components.AppCard
import com.vibra.bus.presentation.components.AppTopBar
import com.vibra.bus.presentation.components.EmptyState
import com.vibra.bus.presentation.components.ShimmerList
import com.vibra.bus.presentation.viewmodel.StopSelectionViewModel
import com.vibra.bus.util.UiState
import org.koin.compose.viewmodel.koinViewModel

data class StopsListScreen(val plate: String) : Screen {

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
            contentWindowInsets = WindowInsets(0),
            topBar = {
                AppTopBar(
                    title = "Paradas",
                    subtitle = busDetail?.name ?: plate,
                    onBack = { navigator.pop() },
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (val state = stopsState) {
                    is UiState.Loading -> ShimmerList(count = 5, itemHeight = 72.dp)
                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            EmptyState(
                                message = "Sin paradas disponibles",
                                subtitle = "No hay paradas registradas para esta ruta",
                                ctaLabel = "Reintentar",
                                onCtaClick = { viewModel.loadStops(plate) },
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                itemsIndexed(state.data, key = { _, s -> s.id }, contentType = { _, _ -> "stop" }) { index, stop ->
                                    StopListItem(
                                        modifier = Modifier.staggerIn(index),
                                        stop = stop,
                                        isFirst = index == 0,
                                        isLast = index == state.data.lastIndex,
                                    )
                                }
                            }
                        }
                    }
                    is UiState.Error -> EmptyState(
                    pose = MascotPose.Sad,
                        message = "No pudimos cargar las paradas",
                        subtitle = state.message,
                        ctaLabel = "Reintentar",
                        onCtaClick = { viewModel.loadStops(plate) },
                    )
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun StopListItem(stop: StopWithPivotDto, isFirst: Boolean, isLast: Boolean, modifier: Modifier = Modifier) {
    val badge = if (isFirst) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    val onBadge = if (isFirst) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary
    val eta = if (stop.estimatedMinutes == 0) "Salida" else "~${stop.estimatedMinutes} min"

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Parada ${stop.order}, ${stop.name}, ${stop.address}, $eta"
            },
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(32.dp).background(badge, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${stop.order}",
                    style = MaterialTheme.typography.labelLarge,
                    color = onBadge,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stop.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stop.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = eta,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
