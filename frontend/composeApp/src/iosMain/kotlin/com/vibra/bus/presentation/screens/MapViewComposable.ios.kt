package com.vibra.bus.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.vibra.bus.util.LatLng
import com.vibra.bus.data.model.StopDto
import com.vibra.bus.data.model.BusSummaryDto
import kotlinx.cinterop.ExperimentalForeignApi
import platform.MapKit.MKMapView

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MapViewComposable(
    modifier: Modifier,
    userLocation: LatLng?,
    showStops: Boolean,
    selectedStop: StopDto?,
    onStopSelected: (StopDto) -> Unit,
    stops: List<StopDto>,
    buses: List<BusSummaryDto>
) {
    UIKitView(
        factory = {
            val mapView = MKMapView()
            // Here you would configure the MKMapView with stops and buses
            mapView
        },
        modifier = modifier,
        update = { mapView ->
            // Update the map view when state changes
        }
    )
}
