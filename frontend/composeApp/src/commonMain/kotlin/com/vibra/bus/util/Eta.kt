package com.vibra.bus.util

import kotlin.math.max
import kotlin.math.roundToInt

private const val DEFAULT_SPEED_KMH = 30.0
private const val MIN_SPEED_KMH = 12.0
private const val MAX_SPEED_KMH = 50.0

/**
 * ETA local en minutos (minimo 1) por distancia y velocidad. La velocidad se acota a un rango
 * urbano para que un bus detenido no de un ETA infinito ni uno rapido un ETA irreal.
 */
fun estimateEtaMinutes(distanceMeters: Double, speedKmh: Double?): Int {
    val v = (speedKmh ?: DEFAULT_SPEED_KMH).coerceIn(MIN_SPEED_KMH, MAX_SPEED_KMH)
    val metersPerMinute = v * 1000.0 / 60.0
    return max(1, (distanceMeters / metersPerMinute).roundToInt())
}

/** Velocidad suavizada (media exponencial) a partir de lo recorrido en [seconds] segundos. */
fun smoothSpeedKmh(previous: Double?, movedMeters: Double, seconds: Double): Double {
    if (seconds <= 0.0) return previous ?: DEFAULT_SPEED_KMH
    val instant = (movedMeters / seconds) * 3.6
    return if (previous == null) instant else previous * 0.6 + instant * 0.4
}

fun formatEta(minutes: Int?): String = when {
    minutes == null -> "Calculando…"
    minutes <= 1 -> "Menos de 1 min"
    else -> "$minutes min"
}
