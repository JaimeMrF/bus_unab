package com.vibra.bus.presentation.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng as GmsLatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.rememberCameraPositionState
import com.vibra.bus.data.model.BusSummaryDto
import com.vibra.bus.data.model.StopDto
import com.vibra.bus.presentation.theme.LocalIsDarkTheme
import com.vibra.bus.util.LatLng
import com.vibra.bus.util.MapStyle

@OptIn(MapsComposeExperimentalApi::class)
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
    path: List<LatLng>?,
) {
    val isDark = LocalIsDarkTheme.current
    val primaryColor = androidx.compose.material3.MaterialTheme.colorScheme.primary

    val defaultPosition = GmsLatLng(7.1166, -73.1056)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            userLocation?.let { GmsLatLng(it.latitude, it.longitude) } ?: defaultPosition,
            15f,
        )
    }

    LaunchedEffect(userLocation) {
        userLocation?.let {
            if (it.latitude < 15.0) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(GmsLatLng(it.latitude, it.longitude), 15f),
                )
            }
        }
    }

    var mapLoaded by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // ── Icono top-down del bus (marker de Google Maps) ─────────────────────────
    // flat=true tiende el sprite sobre el plano del mapa; rotation lo gira con el
    // heading del bus → se ve hacia dónde va sin necesitar 3D.
    val busIcon by produceState<BitmapDescriptor?>(null) {
        value = try {
            val src = BitmapFactory.decodeResource(context.resources, R.drawable.ic_bus_top)
                ?: return@produceState null
            val w = 96 // tamaño del marker (la fuente es 1600×1600)
            BitmapDescriptorFactory.fromBitmap(
                android.graphics.Bitmap.createScaledBitmap(src, w, w, true)
            )
        } catch (_: Exception) {
            null
        }
    }

    Box(modifier = modifier) {

        // ── Mapa base ─────────────────────────────────────────────────────────
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled  = false,
                myLocationButtonEnabled = false,
            ),
            properties = MapProperties(
                mapStyleOptions = if (isDark) MapStyleOptions(MapStyle.json) else null,
            ),
            onMapLoaded = { mapLoaded = true },
        ) {
            // Polilínea de ruta
            path?.let { p ->
                val pts = p.map { GmsLatLng(it.latitude, it.longitude) }
                val outlineColor = if (isDark) Color(0xCCFFFFFF) else Color(0x66000000)
                val accentColor  = if (isDark) Color(0xFFE8A33D) else Color(0xFF3A3226)
                com.google.maps.android.compose.Polyline(
                    points = pts,
                    color  = outlineColor,
                    width  = 18f,
                    jointType = com.google.android.gms.maps.model.JointType.ROUND,
                    startCap  = com.google.android.gms.maps.model.RoundCap(),
                    endCap    = com.google.android.gms.maps.model.RoundCap(),
                )
                com.google.maps.android.compose.Polyline(
                    points = pts,
                    color  = primaryColor,
                    width  = 10f,
                    jointType = com.google.android.gms.maps.model.JointType.ROUND,
                    startCap  = com.google.android.gms.maps.model.RoundCap(),
                    endCap    = com.google.android.gms.maps.model.RoundCap(),
                )
                com.google.maps.android.compose.Polyline(
                    points  = pts,
                    color   = accentColor,
                    width   = 3f,
                    jointType = com.google.android.gms.maps.model.JointType.ROUND,
                    startCap  = com.google.android.gms.maps.model.RoundCap(),
                    endCap    = com.google.android.gms.maps.model.RoundCap(),
                    pattern = listOf(
                        com.google.android.gms.maps.model.Dash(16f),
                        com.google.android.gms.maps.model.Gap(12f),
                    ),
                )
            }

            // Ubicación del usuario
            userLocation?.let { loc ->
                MarkerComposable(
                    state = MarkerState(GmsLatLng(loc.latitude, loc.longitude)),
                    title = "Mi Ubicación",
                ) { UserLocationMarker() }
            }

            // Paradas
            if (showStops) {
                val context = androidx.compose.ui.platform.LocalContext.current
                stops.forEach { stop ->
                    MarkerComposable(
                        state   = MarkerState(GmsLatLng(stop.latitude, stop.longitude)),
                        title   = stop.name,
                        snippet = "Toca para ver en Google Maps",
                        onClick = {
                            onStopSelected(stop)
                            false // false para mostrar el InfoWindow nativo
                        },
                        onInfoWindowClick = {
                            val uri = android.net.Uri.parse("geo:0,0?q=${stop.latitude},${stop.longitude}(${android.net.Uri.encode(stop.name)})")
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                            intent.setPackage("com.google.android.apps.maps")
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val fb = android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=${stop.latitude},${stop.longitude}")
                                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, fb))
                            }
                        }
                    ) { StopMarker(isSelected = selectedStop?.id == stop.id) }
                }
            }

            // ── Buses: marker de Google Maps con la imagen top-down rotada por heading ──
            buses.forEach { bus ->
                val animLat by animateFloatAsState(
                    bus.latitude.toFloat(), tween(1500, easing = LinearEasing), label = "lat_${bus.plate}"
                )
                val animLng by animateFloatAsState(
                    bus.longitude.toFloat(), tween(1500, easing = LinearEasing), label = "lng_${bus.plate}"
                )
                val animatedHeading by animateFloatAsState(
                    targetValue   = bus.heading.toFloat(),
                    animationSpec = tween(800, easing = LinearEasing),
                    label         = "hdg_${bus.plate}",
                )
                val markerState = remember(bus.plate) { MarkerState(GmsLatLng(bus.latitude, bus.longitude)) }
                LaunchedEffect(animLat, animLng) {
                    markerState.position = GmsLatLng(animLat.toDouble(), animLng.toDouble())
                }
                Marker(
                    state    = markerState,
                    title    = bus.plate,
                    icon     = busIcon,
                    rotation = animatedHeading,
                    flat     = true,
                    anchor   = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                    onClick  = { onBusSelected(bus); true },
                )
            }
        }

        // ── Loading overlay ──
        AnimatedVisibility(
            visible = !mapLoaded,
            exit    = fadeOut(animationSpec = tween(400)),
        ) {
            Box(
                modifier         = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color       = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.5.dp,
                )
            }
        }
    }
}
