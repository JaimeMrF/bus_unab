package com.vibra.bus.presentation.map

/** Posicion y rumbo de un bus en un instante. */
data class BusPose(val latitude: Double, val longitude: Double, val heading: Float)

/** Interpola el rumbo por el camino corto (por ejemplo 350 a 10 grados pasa por 0, no por 180). */
fun lerpHeading(from: Float, to: Float, t: Float): Float {
    var delta = (to - from) % 360f
    if (delta > 180f) delta -= 360f
    if (delta < -180f) delta += 360f
    val h = (from + delta * t) % 360f
    return if (h < 0f) h + 360f else h
}

fun lerpPose(from: BusPose, to: BusPose, t: Float): BusPose = BusPose(
    latitude = from.latitude + (to.latitude - from.latitude) * t,
    longitude = from.longitude + (to.longitude - from.longitude) * t,
    heading = lerpHeading(from.heading, to.heading, t),
)

/**
 * Pose mostrada para cada bus en el instante t (0 a 1, lineal). Un bus nuevo aparece directamente
 * en su destino; uno que ya no esta en [to] desaparece.
 */
fun interpolateBuses(from: Map<String, BusPose>, to: Map<String, BusPose>, t: Float): Map<String, BusPose> {
    val clamped = t.coerceIn(0f, 1f)
    return to.mapValues { (plate, target) ->
        val start = from[plate]
        if (start == null) target else lerpPose(start, target, clamped)
    }
}
