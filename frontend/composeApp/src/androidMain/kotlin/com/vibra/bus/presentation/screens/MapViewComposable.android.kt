package com.vibra.bus.presentation.screens

import android.graphics.Point
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import io.github.sceneview.SceneView
import io.github.sceneview.node.ModelNode
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import kotlin.math.PI

private const val BUS_MODEL_PATH = "models/bus_unab_3d.glb"
private val MODEL_SIZE_DP = 90.dp

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

        // ── Overlay 3D ───────────────────────────────────────────────────────
        buses.forEach { bus ->
            val screenPos = busScreenPositions[bus.plate] ?: return@forEach
            val animatedHeading by animateFloatAsState(
                targetValue   = bus.heading.toFloat(),
                animationSpec = tween(800, easing = LinearEasing),
                label         = "hdg_${bus.plate}",
            )

            Bus3DOverlay(
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

// ── Bus 3D con SceneView (API view-based de 0.10.0) ──────────────────────────

@Composable
private fun Bus3DOverlay(heading: Float, modifier: Modifier) {
    // Referencia al ModelNode para actualizar la rotación desde fuera del factory
    val modelNodeRef = remember { mutableStateOf<ModelNode?>(null) }

    AndroidView(
        modifier = modifier,
        factory  = { ctx ->
            SceneView(ctx).also { sv ->
                // Fondo transparente para que se vea el mapa debajo
                sv.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                sv.setZOrderOnTop(true)

                // Carga el GLB directamente desde assets — el engine lo gestiona SceneView
                val node = ModelNode(
                    engine               = sv.engine,
                    modelGlbFileLocation = BUS_MODEL_PATH,
                    autoAnimate          = false,
                    scaleUnits           = 1.4f,
                ).also { n ->
                    // -90° en X = modelo "tumbado" visto desde arriba
                    n.quaternion = headingToQuaternion(-90f, 0f)
                }
                sv.addChildNode(node)
                modelNodeRef.value = node

                // Cámara cenital: por encima del modelo mirando hacia abajo
                sv.camera.position  = Float3(0f, 3f, 0f)
                sv.camera.quaternion = headingToQuaternion(90f, 0f)
            }
        },
        update = { _ ->
            modelNodeRef.value?.quaternion = headingToQuaternion(-90f, heading)
        },
    )

    DisposableEffect(Unit) {
        onDispose { modelNodeRef.value = null }
    }
}

// Convierte ángulos Euler (pitch en X, yaw en Y) a quaternion
private fun headingToQuaternion(pitchDeg: Float, yawDeg: Float): Quaternion {
    val pitch = (pitchDeg * PI / 180.0).toFloat()
    val yaw   = (yawDeg   * PI / 180.0).toFloat()
    val qPitch = Quaternion(Float3(1f, 0f, 0f), pitch)
    val qYaw   = Quaternion(Float3(0f, 1f, 0f), yaw)
    return qYaw * qPitch
}
