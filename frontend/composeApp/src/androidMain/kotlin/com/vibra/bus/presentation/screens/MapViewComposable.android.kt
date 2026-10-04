package com.vibra.bus.presentation.screens

import com.vibra.bus.presentation.theme.appColors
import com.vibra.bus.presentation.motion.LocalMotion
import com.vibra.bus.presentation.map.interpolateBuses
import com.vibra.bus.presentation.map.BusSpriteKey
import com.vibra.bus.presentation.map.BusSpriteCache
import com.vibra.bus.presentation.map.BusPose
import com.vibra.bus.presentation.map.BusMapState
import com.vibra.bus.presentation.map.BusIcon
import com.vibra.bus.domain.brand.parseHexColor
import com.vibra.bus.domain.brand.BusStyle
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.graphics.toArgb
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

// Vista inicial genérica (sin ubicación del usuario); se recentra en cuanto hay GPS o buses.
private const val CITY_LAT = 4.6
private const val CITY_LNG = -74.08
private const val CITY_ZOOM = 11.0

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
private const val IMG_BUS_PREFIX = "img-bus-"

/** Colores del mapa derivados del tema/marca activos (ya no hay colores de marca fijos). */
private class MapColors(val primary: Int, val accent: Int, val onPrimary: Int, val busBody: Int, val busAccent: Int)

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

/**
 * Los buses llevan rumbo y estado como propiedades: la capa rota el sprite y elige la imagen
 * (anillo por estado) sin recrear nada; solo cambia la fuente GeoJSON.
 */
private fun busCollection(poses: Map<String, BusPose>, states: Map<String, BusMapState>): FeatureCollection =
    FeatureCollection.fromFeatures(
        poses.map { (plate, pose) ->
            Feature.fromGeometry(MlLatLng(pose.latitude, pose.longitude).toGeoPoint()).apply {
                addNumberProperty("heading", pose.heading)
                addStringProperty("plate", plate)
                addStringProperty("img", IMG_BUS_PREFIX + (states[plate] ?: BusMapState.Available).key)
            }
        },
    )

private fun userCollection(userLocation: LatLng?): FeatureCollection =
    FeatureCollection.fromFeatures(
        userLocation
            ?.let { listOf(Feature.fromGeometry(MlLatLng(it.latitude, it.longitude).toGeoPoint())) }
            ?: emptyList<Feature>(),
    )

/** Sprites de bus por estado, dibujados una vez por combinacion de color, forma y tamano (cache). */
private fun busSprites(colors: MapColors, style: BusStyle?, ringColors: Map<BusMapState, Int>): Map<String, Bitmap> {
    val icon = BusIcon.parse(style?.icon)
    return BusMapState.values().associate { state ->
        val key = BusSpriteKey(colors.busBody, colors.busAccent, ringColors.getValue(state), state, icon, 96)
        val bmp = BusSpriteCache.get(key).asAndroidBitmap()
        bmp.density = DisplayMetrics.DENSITY_DEFAULT
        (IMG_BUS_PREFIX + state.key) to bmp
    }
}

/** Instala fuentes y capas una sola vez por style (setStyle resetea todo). */
private fun Style.installLayers(sprites: Map<String, Bitmap>, colors: MapColors) {
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
                PropertyFactory.lineColor(colors.primary),
                PropertyFactory.lineWidth(4f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            )
        },
    )
    addLayer(
        LineLayer(LYR_ROUTE_DASH, SRC_ROUTE).apply {
            setProperties(
                PropertyFactory.lineColor(colors.onPrimary),
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
                PropertyFactory.circleStrokeColor(colors.primary),
                PropertyFactory.circleStrokeWidth(2.5f),
            )
        },
    )
    addSource(GeoJsonSource(SRC_STOP_SEL, FeatureCollection.fromFeatures(emptyList<Feature>())))
    addLayer(
        CircleLayer(LYR_STOP_SEL, SRC_STOP_SEL).apply {
            setProperties(
                PropertyFactory.circleRadius(9f),
                PropertyFactory.circleColor(colors.accent),
                PropertyFactory.circleStrokeColor(colors.primary),
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
                PropertyFactory.circleColor(colors.accent),
                PropertyFactory.circleStrokeColor(0xFFFFFFFF.toInt()),
                PropertyFactory.circleStrokeWidth(3f),
            )
        },
    )

    // ── Buses: símbolo rotado por el rumbo (alineado al mapa) ────────────────
    sprites.forEach { (id, bmp) -> addImage(id, bmp, false) }
    addSource(GeoJsonSource(SRC_BUSES, FeatureCollection.fromFeatures(emptyList<Feature>())))
    addLayer(
        SymbolLayer(LYR_BUSES, SRC_BUSES).apply {
            setProperties(
                PropertyFactory.iconImage(Expression.get("img")),
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
    busStates: Map<String, BusMapState>,
    busStyle: BusStyle?,
) {
    val isDark = LocalIsDarkTheme.current
    val scheme = MaterialTheme.colorScheme
    val app = MaterialTheme.appColors
    // Cuerpo y acento del bus: bus_style del tenant o, si es null, primary y secondary.
    val busBody = busStyle?.body?.let { parseHexColor(it, scheme.primary) } ?: scheme.primary
    val busAccent = busStyle?.accent?.let { parseHexColor(it, scheme.secondary) } ?: scheme.secondary
    val mapColors = remember(scheme.primary, scheme.tertiary, scheme.onPrimary, busBody, busAccent) {
        MapColors(scheme.primary.toArgb(), scheme.tertiary.toArgb(), scheme.onPrimary.toArgb(), busBody.toArgb(), busAccent.toArgb())
    }
    val ringColors = remember(app.success, app.busFull, app.warning) {
        mapOf(
            BusMapState.Available to app.success.toArgb(),
            BusMapState.Full to app.busFull.toArgb(),
            BusMapState.Arriving to app.warning.toArgb(),
        )
    }
    val sprites = remember(mapColors, busStyle?.icon, ringColors) { busSprites(mapColors, busStyle, ringColors) }
    val motion = LocalMotion.current
    // Ultima pose dibujada de cada bus: punto de partida de la siguiente interpolacion.
    val displayed = remember { HashMap<String, BusPose>() }
    val context = LocalContext.current
    val lifecycleOwner = remember(context) { context as? LifecycleOwner }

    // MapLibre exige inicializarse una vez antes de instanciar un MapView.
    remember(context) {
        MapLibre.getInstance(context)
        true
    }

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
    LaunchedEffect(map, isDark, sprites) {
        val mlMap = map ?: return@LaunchedEffect
        mapLoaded = false
        mlMap.setStyle(Style.Builder().fromUri(if (isDark) STYLE_DARK else STYLE_LIGHT)) { loaded ->
            loaded.installLayers(sprites, mapColors)
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
    // Buses: movimiento lineal de 1 s entre posiciones (sin saltos). Solo se actualiza la fuente
    // GeoJSON, a ~30 fps mientras dura la animacion; ni capas ni imagenes se recrean.
    LaunchedEffect(style, buses, busStates, motion.animate) {
        val source = style?.getSourceAs<GeoJsonSource>(SRC_BUSES) ?: return@LaunchedEffect
        val target = buses.associate { it.plate to BusPose(it.latitude, it.longitude, it.heading.toFloat()) }
        val from = HashMap(displayed)
        if (!motion.animate || from.isEmpty()) {
            displayed.clear(); displayed.putAll(target)
            source.setGeoJson(busCollection(target, busStates))
            return@LaunchedEffect
        }
        val startNanos = withFrameNanos { it }
        var lastDraw = 0L
        while (true) {
            val now = withFrameNanos { it }
            val t = ((now - startNanos) / 1_000_000_000f).coerceIn(0f, 1f)
            if (t >= 1f || now - lastDraw >= 33_000_000L) {
                val poses = interpolateBuses(from, target, t)
                displayed.clear(); displayed.putAll(poses)
                source.setGeoJson(busCollection(poses, busStates))
                lastDraw = now
            }
            if (t >= 1f) break
        }
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
