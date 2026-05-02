package com.vibra.bus.presentation.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng as GmsLatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.vibra.bus.util.LatLng
import com.vibra.bus.data.model.StopDto
import com.vibra.bus.data.model.BusSummaryDto

import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.MapProperties
import com.vibra.bus.util.MapStyle

@Composable
actual fun MapViewComposable(
    modifier: Modifier,
    userLocation: LatLng?,
    showStops: Boolean,
    selectedStop: StopDto?,
    onStopSelected: (StopDto) -> Unit,
    selectedBus: BusSummaryDto?,
    onBusSelected: (BusSummaryDto) -> Unit,
    stops: List<StopDto>,
    buses: List<BusSummaryDto>,
    path: List<LatLng>?
) {
    val defaultPosition = GmsLatLng(7.1166, -73.1056) // UNAB Jardín, Bucaramanga
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            userLocation?.let { GmsLatLng(it.latitude, it.longitude) } ?: defaultPosition,
            15f,
        )
    }

    LaunchedEffect(userLocation) {
        userLocation?.let {
            // Solo centramos automáticamente si la ubicación es "válida" para Bucaramanga
            // Evitamos saltos a USA (lat ~37) si el usuario está en Colombia (lat ~7)
            if (it.latitude < 15.0) { 
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(GmsLatLng(it.latitude, it.longitude), 15f),
                )
            }
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
        ),
        properties = MapProperties(
            mapStyleOptions = MapStyleOptions(MapStyle.json),
            isMyLocationEnabled = true
        )
    ) {
        // Draw Path (Polyline)
        path?.let { p ->
            com.google.maps.android.compose.Polyline(
                points = p.map { GmsLatLng(it.latitude, it.longitude) },
                color = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                width = 12f,
                jointType = com.google.android.gms.maps.model.JointType.ROUND,
                pattern = listOf(com.google.android.gms.maps.model.Dot())
            )
        }

        userLocation?.let { loc ->
            com.google.maps.android.compose.MarkerComposable(
                state = com.google.maps.android.compose.MarkerState(position = GmsLatLng(loc.latitude, loc.longitude)),
                title = "Mi Ubicación"
            ) {
                UserLocationMarker()
            }
        }

        if (showStops) {
            stops.forEach { stop ->
                val isSelected = selectedStop?.id == stop.id
                com.google.maps.android.compose.MarkerComposable(
                    state = com.google.maps.android.compose.MarkerState(position = GmsLatLng(stop.latitude, stop.longitude)),
                    title = stop.name,
                    onClick = {
                        onStopSelected(stop)
                        true
                    }
                ) {
                    StopMarker(isSelected = isSelected)
                }
            }
        }

        buses.forEach { bus ->
            AnimatedBusMarker(
                bus = bus,
                isSelected = selectedBus?.plate == bus.plate,
                onBusSelected = onBusSelected
            )
        }
    }
}

@Composable
private fun AnimatedBusMarker(
    bus: BusSummaryDto,
    isSelected: Boolean,
    onBusSelected: (BusSummaryDto) -> Unit
) {
    // Remember marker state per bus plate so it persists across recompositions
    val markerState = remember(bus.plate) {
        MarkerState(position = GmsLatLng(bus.latitude, bus.longitude))
    }

    // Animate lat/lng smoothly between GPS updates (matches the 10s polling interval)
    val animatedLat by animateFloatAsState(
        targetValue = bus.latitude.toFloat(),
        animationSpec = tween(durationMillis = 1500, easing = LinearEasing),
        label = "bus_lat_${bus.plate}"
    )
    val animatedLng by animateFloatAsState(
        targetValue = bus.longitude.toFloat(),
        animationSpec = tween(durationMillis = 1500, easing = LinearEasing),
        label = "bus_lng_${bus.plate}"
    )

    LaunchedEffect(animatedLat, animatedLng) {
        markerState.position = GmsLatLng(animatedLat.toDouble(), animatedLng.toDouble())
    }

    com.google.maps.android.compose.MarkerComposable(
        state = markerState,
        title = bus.plate,
        onClick = {
            onBusSelected(bus)
            true
        }
    ) {
        BusMarker(
            isSelected = isSelected,
            heading = bus.heading.toFloat()
        )
    }
}
