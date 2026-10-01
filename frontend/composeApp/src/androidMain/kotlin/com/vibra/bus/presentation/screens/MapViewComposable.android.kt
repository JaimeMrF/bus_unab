package com.vibra.bus.presentation.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.DisplayMetrics
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.vibra.bus.R
import com.vibra.bus.data.model.BusSummaryDto
import com.vibra.bus.data.model.StopDto
import com.vibra.bus.presentation.theme.LocalIsDarkTheme
import com.vibra.bus.util.LatLng
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng as MlLatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

// ── Tiles: OpenFreeMap (OpenStreetMap vectorial) — sin API key, sin cuenta ────
// Si algún día hay más tráfico del tolerado, se auto-hospeda el estilo.
private const val STYLE_LIGHT = "https://tiles.openfreemap.org/styles/liberty"
private const val STYLE_DARK = "https://tiles.openfreemap.org/styles/dark"

// Centro de Bucaramanga (Parque Santander aprox.)
private const val CITY_LAT = 7.1166
private const val CITY_LNG = -73.1056
private const val CITY_ZOOM = 15.0

private const val SRC_ROUTE = "src-route"
private const val LYR_ROUTE_HALO = "lyr-route-halo"
private const val LYR_ROUTE_MAIN = "lyr-route-main"
private const val LYR_ROUTE_DASH = "lyr-route-dash"
private const val SRC_STOPS = "src-stops"
private const val LYR_STOPS = "lyr-stops"
private const val SRC_STOP_SEL = "src-stop-selected"
private const val LYR_STOP_SEL = "lyr-stop-selected"
private const val SRC_USER = "src-user"
private const val LYR_USER = "lyr-user"
private const val SRC_BUSES = "src-buses"
private const val LYR_BUSES = "lyr-buses"
private const val IMG_BUS = "img-bus"

private val BRAND_BLUE = 0xFF01265A.toInt()
private val BRAND_YELLOW = 0xFFFCBB01.toInt()
private val USER_BLUE = 0xFF2A6FD6.toInt()

private fun MlLatLng.toGeoPoint(): Point = Point.fromLngLat(longitude, latitude)

private fun routeCollection(path: List<LatLng>?): FeatureCollection {
    if (path.isNullOrEmpty()) return FeatureCollection.fromFeatures(emptyList<Feature>())
    val points = path.map { Point.fromLngLat(it.longitude, it.latitude) }
    return FeatureCollection.fromFeatures(
        listOf(Feature.fromGeometry(LineString.fromLngLats(points))),
    )
}

private fun stopCollection(stops: List<StopDto>): FeatureCollection =
    FeatureCollection.fromFeatures(
        stops.map { stop ->
            Feature.fromGeometry(MlLatLng(stop.latitude, stop.longitude).toGeoPoint()).apply {
                addNumberProperty("stopId", stop.id)
                addStringProperty("stopName", stop.name)
            }
        },
    )

/** El bus lleva su rumbo como propiedad para que la capa lo rote en el mapa. */
private fun busCollection(buses: List<BusSummaryDto>): FeatureCollection =
    FeatureCollection.fromFeatures(
        buses.map { bus ->
            Feature.fromGeometry(MlLatLng(bus.latitude, bus.longitude).toGeoPoint()).apply {
                addNumberProperty("heading", bus.heading)
                addStringProperty("plate", bus.plate)
            }
        },
    )

private fun userCollection(userLocation: LatLng?): FeatureCollection =
    FeatureCollection.fromFeatures(
        userLocation
            ?.let { listOf(Feature.fromGeometry(MlLatLng(it.latitude, it.longitude).toGeoPoint())) }
            ?: emptyList<Feature>(),
    )

/**
 * Sprite top-down del bus. Se normaliza a 96px con densidad mdpi para que
 * `iconSize` sea predecible sin importar la densidad del dispositivo.
 */
private fun busBitmap(context: Context): Bitmap {
    val src = BitmapFactory.decodeResource(context.resources, R.drawable.ic_bus_top)
        ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    val scaled = Bitmap.createScaledBitmap(src, 96, 96, true)
    scaled.density = DisplayMetrics.DENSITY_DEFAULT
    return scaled
}

/** Instala fuentes y capas una sola vez por style (setStyle resetea todo). */
private fun Style.installLayers(bus: Bitmap) {
    if (getSourceAs<GeoJsonSource>(SRC_BUSES) != null) return

    // ── Ruta: halo + línea + guía punteada ───────────────────────────────────
    addSource(GeoJsonSource(SRC_ROUTE, FeatureCollection.fromFeatures(emptyList<Feature>())))
    addLayer(
        LineLayer(LYR_ROUTE_HALO, SRC_ROUTE).apply {
            setProperties(
                PropertyFactory.lineColor(0x66000000),
                PropertyFactory.lineWidth(7f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            )
        },
    )
    addLayer(
        LineLayer(LYR_ROUTE_MAIN, SRC_ROUTE).apply {
            setProperties(
                PropertyFactory.lineColor(BRAND_BLUE),
                PropertyFactory.lineWidth(4f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            )
        },
    )
    addLayer(
        LineLayer(LYR_ROUTE_DASH, SRC_ROUTE).apply {
            setProperties(
                PropertyFactory.lineColor(BRAND_YELLOW),
                PropertyFactory.lineWidth(1.5f),
                PropertyFactory.lineDasharray(arrayOf(2f, 2f)),
            )
        },
    )

    // ── Paradas (capa aparte para la seleccionada) ───────────────────────────
    addSource(GeoJsonSource(SRC_STOPS, FeatureCollection.fromFeatures(emptyList<Feature>())))
    addLayer(
        CircleLayer(LYR_STOPS, SRC_STOPS).apply {
            setProperties(
                PropertyFactory.circleRadius(6f),
                PropertyFactory.circleColor(0xFFFFFFFF.toInt()),
                PropertyFactory.circleStrokeColor(BRAND_BLUE),
                PropertyFactory.circleStrokeWidth(2.5f),
            )
        },
    )
    addSource(GeoJsonSource(SRC_STOP_SEL, FeatureCollection.fromFeatures(emptyList<Feature>())))
    addLayer(
        CircleLayer(LYR_STOP_SEL, SRC_STOP_SEL).apply {
            setProperties(
                PropertyFactory.circleRadius(9f),
                PropertyFactory.circleColor(BRAND_YELLOW),
                PropertyFactory.circleStrokeColor(BRAND_BLUE),
                PropertyFactory.circleStrokeWidth(3f),
            )
        },
    )

    // ── Ubicación del usuario ────────────────────────────────────────────────
    addSource(GeoJsonSource(SRC_USER, FeatureCollection.fromFeatures(emptyList<Feature>())))
    addLayer(
        CircleLayer(LYR_USER, SRC_USER).apply {
            setProperties(
                PropertyFactory.circleRadius(7f),
                PropertyFactory.circleColor(USER_BLUE),
                PropertyFactory.circleStrokeColor(0xFFFFFFFF.toInt()),
                PropertyFactory.circleStrokeWidth(3f),
            )
        },
    )

    // ── Buses: símbolo rotado por el rumbo (alineado al mapa) ────────────────
    addImage(IMG_BUS, bus, true)
    addSource(GeoJsonSource(SRC_BUSES, FeatureCollection.fromFeatures(emptyList<Feature>())))
    addLayer(
        SymbolLayer(LYR_BUSES, SRC_BUSES).apply {
            setProperties(
                PropertyFactory.iconImage(IMG_BUS),
                PropertyFactory.iconRotate(Expression.get("heading")),
                PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER),
                PropertyFactory.iconSize(0.55f), // ← perilla: tamaño del bus en pantalla
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
            )
        },
    )
}

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
    val context = LocalContext.current
    val lifecycleOwner = remember(context) { context as? LifecycleOwner }

    // MapLibre exige inicializarse una vez antes de instanciar un MapView.
    remember(context) {
        MapLibre.getInstance(context)
        true
    }

    val busSprite = remember(context) { busBitmap(context) }
    val mapView = remember(context) {
        MapView(
            context,
            MapLibreMapOptions.createFromAttributes(context).camera(
                CameraPosition.Builder()
                    .target(MlLatLng(CITY_LAT, CITY_LNG))
                    .zoom(CITY_ZOOM)
                    .build(),
            ),
        )
    }

    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    var mapLoaded by remember { mutableStateOf(false) }

    // El listener se registra una sola vez: lee el estado vivo por referencia.
    val stopsState = rememberUpdatedState(stops)
    val busesState = rememberUpdatedState(buses)
    val onStopSelectedState = rememberUpdatedState(onStopSelected)
    val onBusSelectedState = rememberUpdatedState(onBusSelected)

    DisposableEffect(lifecycleOwner, mapView) {
        mapView.onCreate(null)
        if (lifecycleOwner == null) {
            mapView.onStart()
            mapView.onResume()
            onDispose {
                mapView.onPause()
                mapView.onStop()
                mapView.onDestroy()
            }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                mapView.onStop()
                mapView.onDestroy()
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = {
                mapView.getMapAsync { mlMap ->
                    map = mlMap
                    mlMap.uiSettings.apply {
                        // Vista siempre top: sin giro ni inclinación.
                        setRotateGesturesEnabled(false)
                        setTiltGesturesEnabled(false)
                        setCompassEnabled(false)
                    }
                    mlMap.addOnMapClickListener { latLng ->
                        val point = mlMap.projection.toScreenLocation(latLng)

                        val hitStop = mlMap.queryRenderedFeatures(point, LYR_STOPS).firstOrNull()
                        val stopId = hitStop?.getNumberProperty("stopId")?.toInt()
                        val stop = stopsState.value.firstOrNull { it.id == stopId }
                        if (stop != null) {
                            onStopSelectedState.value(stop)
                            return@addOnMapClickListener true
                        }

                        val plate = mlMap.queryRenderedFeatures(point, LYR_BUSES)
                            .firstOrNull()?.getStringProperty("plate")
                        val bus = busesState.value.firstOrNull { it.plate == plate }
                        if (bus != null) {
                            onBusSelectedState.value(bus)
                            return@addOnMapClickListener true
                        }
                        false
                    }
                }
                mapView
            },
            modifier = Modifier.fillMaxSize(),
            update = { },
        )

        // ── Loading overlay ──────────────────────────────────────────────────
        AnimatedVisibility(visible = !mapLoaded, exit = fadeOut(animationSpec = tween(400))) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.5.dp,
                )
            }
        }
    }

    // ── Estilo (se re-aplica si cambia el tema) ──────────────────────────────
    LaunchedEffect(map, isDark) {
        val mlMap = map ?: return@LaunchedEffect
        mapLoaded = false
        mlMap.setStyle(Style.Builder().fromUri(if (isDark) STYLE_DARK else STYLE_LIGHT)) { loaded ->
            loaded.installLayers(busSprite)
            style = loaded
            mapLoaded = true
        }
    }

    // ── Datos → fuentes GeoJSON ──────────────────────────────────────────────
    LaunchedEffect(style, path) {
        style?.getSourceAs<GeoJsonSource>(SRC_ROUTE)?.setGeoJson(routeCollection(path))
    }
    LaunchedEffect(style, stops, showStops) {
        val visible = if (showStops) stops else emptyList()
        style?.getSourceAs<GeoJsonSource>(SRC_STOPS)?.setGeoJson(stopCollection(visible))
    }
    LaunchedEffect(style, selectedStop) {
        val selected = selectedStop?.let { listOf(it) } ?: emptyList()
        style?.getSourceAs<GeoJsonSource>(SRC_STOP_SEL)?.setGeoJson(stopCollection(selected))
    }
    LaunchedEffect(style, userLocation) {
        style?.getSourceAs<GeoJsonSource>(SRC_USER)?.setGeoJson(userCollection(userLocation))
    }
    LaunchedEffect(style, buses) {
        style?.getSourceAs<GeoJsonSource>(SRC_BUSES)?.setGeoJson(busCollection(buses))
    }

    // ── Cámara: sigue al usuario (descarta el (0,0) que devuelve el backend) ─
    LaunchedEffect(map, userLocation) {
        val mlMap = map ?: return@LaunchedEffect
        userLocation?.takeIf { it.latitude < 15.0 }?.let {
            mlMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(MlLatLng(it.latitude, it.longitude), CITY_ZOOM),
            )
        }
    }
}
