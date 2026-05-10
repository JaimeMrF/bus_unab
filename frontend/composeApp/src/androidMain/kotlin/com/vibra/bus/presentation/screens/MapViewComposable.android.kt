package com.vibra.bus.presentation.screens

import android.graphics.Point
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng as GmsLatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.rememberCameraPositionState
import com.vibra.bus.data.model.BusSummaryDto
import com.vibra.bus.data.model.StopDto
import com.vibra.bus.util.LatLng
import com.vibra.bus.util.MapStyle

private val MODEL_SIZE_DP = 53.dp

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

    var busScreenPositions by remember { mutableStateOf(emptyMap<String, Point>()) }

    val density     = LocalDensity.current
    val modelHalfPx = with(density) { (MODEL_SIZE_DP / 2).roundToPx() }

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
                mapStyleOptions = MapStyleOptions(MapStyle.json),
            ),
        ) {
            // Polilínea de ruta
            path?.let { p ->
                val pts = p.map { GmsLatLng(it.latitude, it.longitude) }
                com.google.maps.android.compose.Polyline(
                    points = pts,
                    color  = androidx.compose.ui.graphics.Color(0xCCFFFFFF),
                    width  = 18f,
                    jointType = com.google.android.gms.maps.model.JointType.ROUND,
                    startCap  = com.google.android.gms.maps.model.RoundCap(),
                    endCap    = com.google.android.gms.maps.model.RoundCap(),
                )
                com.google.maps.android.compose.Polyline(
                    points = pts,
                    color  = androidx.compose.ui.graphics.Color(0xFF5B2C8C),
                    width  = 10f,
                    jointType = com.google.android.gms.maps.model.JointType.ROUND,
                    startCap  = com.google.android.gms.maps.model.RoundCap(),
                    endCap    = com.google.android.gms.maps.model.RoundCap(),
                )
                com.google.maps.android.compose.Polyline(
                    points  = pts,
                    color   = androidx.compose.ui.graphics.Color(0xFFE9A427),
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
                stops.forEach { stop ->
                    MarkerComposable(
                        state   = MarkerState(GmsLatLng(stop.latitude, stop.longitude)),
                        title   = stop.name,
                        onClick = { onStopSelected(stop); true },
                    ) { StopMarker(isSelected = selectedStop?.id == stop.id) }
                }
            }

            // Marcadores invisibles para capturar clicks en buses
            buses.forEach { bus ->
                val animLat by animateFloatAsState(
                    bus.latitude.toFloat(), tween(1500, easing = LinearEasing), label = "lat_${bus.plate}"
                )
                val animLng by animateFloatAsState(
                    bus.longitude.toFloat(), tween(1500, easing = LinearEasing), label = "lng_${bus.plate}"
                )
                val markerState = remember(bus.plate) { MarkerState(GmsLatLng(bus.latitude, bus.longitude)) }
                LaunchedEffect(animLat, animLng) {
                    markerState.position = GmsLatLng(animLat.toDouble(), animLng.toDouble())
                }
                com.google.maps.android.compose.Marker(
                    state   = markerState,
                    title   = bus.plate,
                    alpha   = 0f,
                    anchor  = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                    onClick = { onBusSelected(bus); true },
                )
            }

            // Actualizar posiciones en pantalla al mover la cámara
            MapEffect(buses) { map ->
                fun update() {
                    val proj = map.projection
                    busScreenPositions = buses.associate { bus ->
                        bus.plate to proj.toScreenLocation(GmsLatLng(bus.latitude, bus.longitude))
                    }
                }
                map.setOnCameraIdleListener  { update() }
                map.setOnCameraMoveListener  { update() }
                update()
            }
        }

        // ── Overlay buses (Canvas Compose — sin SurfaceView) ─────────────────
        buses.forEach { bus ->
            key(bus.plate) {
                busScreenPositions[bus.plate]?.let { screenPos ->
                    val animatedHeading by animateFloatAsState(
                        targetValue   = bus.heading.toFloat(),
                        animationSpec = tween(800, easing = LinearEasing),
                        label         = "hdg_${bus.plate}",
                    )

                    BusIconOverlay(
                        heading  = animatedHeading,
                        modifier = Modifier
                            .size(MODEL_SIZE_DP)
                            .offset {
                                IntOffset(
                                    screenPos.x - modelHalfPx,
                                    screenPos.y - modelHalfPx,
                                )
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun BusIconOverlay(heading: Float, modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width  / 2f
            val cy = size.height / 2f
            val s  = minOf(size.width, size.height)

            rotate(degrees = heading, pivot = Offset(cx, cy)) {

                // Dimensiones del bus dentro del canvas cuadrado
                val bW = s * 0.48f          // ancho carrocería
                val bH = s * 0.78f          // largo carrocería
                val bL = cx - bW / 2f       // borde izquierdo
                val bT = cy - bH / 2f       // borde frontal
                val bR = bL + bW            // borde derecho
                val bB = bT + bH            // borde trasero
                val cr = s * 0.09f          // radio esquinas cuerpo

                // — Sombra —
                drawRoundRect(
                    color        = Color(0x40000000),
                    topLeft      = Offset(bL + 2.5f, bT + 2.5f),
                    size         = Size(bW, bH),
                    cornerRadius = CornerRadius(cr),
                )

                // — Borde blanco: contraste sobre cualquier fondo de mapa —
                drawRoundRect(
                    color        = Color(0xFFFFFFFF),
                    topLeft      = Offset(bL - 2f, bT - 2f),
                    size         = Size(bW + 4f, bH + 4f),
                    cornerRadius = CornerRadius(cr + 1.5f),
                )

                // — Carrocería base (morado UNAB) —
                drawRoundRect(
                    color        = Color(0xFF5B2C8C),
                    topLeft      = Offset(bL, bT),
                    size         = Size(bW, bH),
                    cornerRadius = CornerRadius(cr),
                )

                // — Panel de techo (ligeramente más oscuro, da profundidad) —
                drawRoundRect(
                    color        = Color(0xFF4A2275),
                    topLeft      = Offset(bL + bW * 0.13f, bT + bH * 0.11f),
                    size         = Size(bW * 0.74f, bH * 0.78f),
                    cornerRadius = CornerRadius(cr * 0.55f),
                )

                // — Franja lateral UNAB (amarilla, horizontal) —
                drawRect(
                    color   = Color(0xFFE9A427),
                    topLeft = Offset(bL, cy - s * 0.052f),
                    size    = Size(bW, s * 0.104f),
                )

                // — Parabrisas delantero —
                drawRoundRect(
                    color        = Color(0xCCADD8FF),
                    topLeft      = Offset(bL + bW * 0.10f, bT + bH * 0.025f),
                    size         = Size(bW * 0.80f, bH * 0.115f),
                    cornerRadius = CornerRadius(s * 0.03f),
                )
                // Reflejo sutil en el parabrisas
                drawRoundRect(
                    color        = Color(0x55FFFFFF),
                    topLeft      = Offset(bL + bW * 0.12f, bT + bH * 0.03f),
                    size         = Size(bW * 0.28f, bH * 0.06f),
                    cornerRadius = CornerRadius(s * 0.02f),
                )

                // — Ventana trasera —
                drawRoundRect(
                    color        = Color(0x88ADD8FF),
                    topLeft      = Offset(bL + bW * 0.14f, bT + bH * 0.855f),
                    size         = Size(bW * 0.72f, bH * 0.09f),
                    cornerRadius = CornerRadius(s * 0.02f),
                )

                // — Ventanas laterales (4 por lado) —
                val winW  = bW * 0.115f
                val winH  = bH * 0.085f
                val winXL = bL + bW * 0.045f
                val winXR = bR - bW * 0.045f - winW
                listOf(0.225f, 0.360f, 0.510f, 0.645f).forEach { yRel ->
                    val winY = bT + bH * yRel
                    drawRoundRect(
                        color        = Color(0x99A8D4FF),
                        topLeft      = Offset(winXL, winY),
                        size         = Size(winW, winH),
                        cornerRadius = CornerRadius(s * 0.015f),
                    )
                    drawRoundRect(
                        color        = Color(0x99A8D4FF),
                        topLeft      = Offset(winXR, winY),
                        size         = Size(winW, winH),
                        cornerRadius = CornerRadius(s * 0.015f),
                    )
                }

                // — Ruedas (4 arcos, dos ejes) —
                val wR  = s * 0.052f
                val wYf = bT + bH * 0.245f   // eje delantero
                val wYr = bT + bH * 0.720f   // eje trasero
                listOf(bL - wR * 0.45f, bR - wR * 0.55f).forEach { wX ->
                    // llanta exterior (negro)
                    drawCircle(Color(0xFF1C0D33), radius = wR,           center = Offset(wX, wYf))
                    drawCircle(Color(0xFF1C0D33), radius = wR,           center = Offset(wX, wYr))
                    // rin interior (gris plata)
                    drawCircle(Color(0xFFBBBBBB), radius = wR * 0.52f,  center = Offset(wX, wYf))
                    drawCircle(Color(0xFFBBBBBB), radius = wR * 0.52f,  center = Offset(wX, wYr))
                }

                // — Faros delanteros —
                val headY  = bT + bH * 0.038f
                val headR  = s * 0.04f
                drawCircle(Color(0xFFFFE680), radius = headR, center = Offset(bL + bW * 0.18f, headY))
                drawCircle(Color(0xFFFFE680), radius = headR, center = Offset(bR - bW * 0.18f, headY))

                // — Flecha de dirección (frente) —
                val aHalf = bW * 0.20f
                val aTip  = bT - s * 0.025f
                val aBase = bT + bH * 0.015f
                drawPath(
                    path = Path().apply {
                        moveTo(cx, aTip)
                        lineTo(cx - aHalf, aBase)
                        lineTo(cx + aHalf, aBase)
                        close()
                    },
                    color = Color(0xFFE9A427),
                )
            }
        }
    }
}
