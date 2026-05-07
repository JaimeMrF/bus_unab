package com.vibra.bus.presentation.screens

import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng as GmsLatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.vibra.bus.data.model.BusSummaryDto
import com.vibra.bus.data.model.StopDto
import com.vibra.bus.util.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.MapProperties
import com.vibra.bus.util.MapStyle

private const val BUS_ASSET_PATH =
    "composeResources/vibrabus.composeapp.generated.resources/drawable/ic_bus_top.webp"

private data class BusIcons(val normal: BitmapDescriptor?, val selected: BitmapDescriptor?)

// ─── Public expect implementation ────────────────────────────────────────────

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
    val context = LocalContext.current
    val defaultPosition = GmsLatLng(7.1166, -73.1056)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            userLocation?.let { GmsLatLng(it.latitude, it.longitude) } ?: defaultPosition,
            15f,
        )
    }

    // Load raw bitmap during composition (no Maps dependency).
    val rawBitmap = remember {
        try {
            context.assets.open(BUS_ASSET_PATH).use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) {
            null
        }
    }

    // BitmapDescriptors are created in onMapLoaded where BitmapDescriptorFactory is guaranteed ready.
    var busIcons by remember { mutableStateOf(BusIcons(null, null)) }

    LaunchedEffect(userLocation) {
        userLocation?.let {
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
        ),
        onMapLoaded = {
            if (rawBitmap != null && busIcons.normal == null) {
                val density = context.resources.displayMetrics.density
                val sizePx = (72 * density).toInt()
                val scaled = android.graphics.Bitmap.createScaledBitmap(rawBitmap, sizePx, sizePx, true)

                val normalDesc = BitmapDescriptorFactory.fromBitmap(scaled)

                val selectedBitmap = scaled.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
                AndroidCanvas(selectedBitmap).drawBitmap(
                    scaled, 0f, 0f,
                    Paint().apply {
                        colorFilter = PorterDuffColorFilter(0xFF6200EE.toInt(), PorterDuff.Mode.SRC_ATOP)
                    }
                )
                val selectedDesc = BitmapDescriptorFactory.fromBitmap(selectedBitmap)

                busIcons = BusIcons(normalDesc, selectedDesc)
            }
        }
    ) {
        path?.let { p ->
            com.google.maps.android.compose.Polyline(
                points    = p.map { GmsLatLng(it.latitude, it.longitude) },
                color     = androidx.compose.ui.graphics.Color(0x4D6200EE),
                width     = 22f,
                jointType = com.google.android.gms.maps.model.JointType.ROUND,
                startCap  = com.google.android.gms.maps.model.RoundCap(),
                endCap    = com.google.android.gms.maps.model.RoundCap(),
            )
            com.google.maps.android.compose.Polyline(
                points    = p.map { GmsLatLng(it.latitude, it.longitude) },
                color     = androidx.compose.ui.graphics.Color(0xFF6200EE),
                width     = 12f,
                jointType = com.google.android.gms.maps.model.JointType.ROUND,
                startCap  = com.google.android.gms.maps.model.RoundCap(),
                endCap    = com.google.android.gms.maps.model.RoundCap(),
            )
        }

        userLocation?.let { loc ->
            com.google.maps.android.compose.MarkerComposable(
                state = MarkerState(position = GmsLatLng(loc.latitude, loc.longitude)),
                title = "Mi Ubicación"
            ) {
                UserLocationMarker()
            }
        }

        if (showStops) {
            stops.forEach { stop ->
                val isSelected = selectedStop?.id == stop.id
                com.google.maps.android.compose.MarkerComposable(
                    state = MarkerState(position = GmsLatLng(stop.latitude, stop.longitude)),
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
                onBusSelected = onBusSelected,
                normalIcon = busIcons.normal,
                selectedIcon = busIcons.selected,
            )
        }
    }
}

// ─── Animated bus marker ──────────────────────────────────────────────────────

@Composable
@GoogleMapComposable
private fun AnimatedBusMarker(
    bus: BusSummaryDto,
    isSelected: Boolean,
    onBusSelected: (BusSummaryDto) -> Unit,
    normalIcon: BitmapDescriptor?,
    selectedIcon: BitmapDescriptor?,
) {
    val markerState = remember(bus.plate) {
        MarkerState(position = GmsLatLng(bus.latitude, bus.longitude))
    }

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
    val animatedHeading by animateFloatAsState(
        targetValue = bus.heading.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = LinearEasing),
        label = "bus_heading_${bus.plate}"
    )

    LaunchedEffect(animatedLat, animatedLng) {
        markerState.position = GmsLatLng(animatedLat.toDouble(), animatedLng.toDouble())
    }

    val icon = (if (isSelected) selectedIcon else normalIcon) ?: return

    Marker(
        state = markerState,
        title = bus.plate,
        icon = icon,
        rotation = animatedHeading,
        flat = true,
        anchor = Offset(0.5f, 0.5f),
        onClick = {
            onBusSelected(bus)
            true
        }
    )
}
