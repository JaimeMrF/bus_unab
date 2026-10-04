package com.vibra.bus.presentation.map

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.vibra.bus.data.model.OccupancyDto

/** Estado visual del bus en el mapa; cambia el anillo del marcador. */
enum class BusMapState(val key: String) {
    Available("available"),
    Full("full"),
    Arriving("arriving"),
}

/** Silueta del bus (vista cenital): classic, modern o minibus. */
enum class BusIcon {
    Classic, Modern, Minibus;

    companion object {
        fun parse(raw: String?): BusIcon = when (raw?.lowercase()) {
            "modern" -> Modern
            "minibus" -> Minibus
            else -> Classic
        }
    }
}

/** Estados de mapa derivados de la ocupación: "full" marca el bus como lleno. */
fun busStatesFrom(occupancy: Map<String, OccupancyDto>): Map<String, BusMapState> =
    occupancy.mapNotNull { (plate, o) -> if (o.level == "full") plate to BusMapState.Full else null }.toMap()

/**
 * Clave de cache del sprite. Los colores van como ARGB entero para que dos paletas iguales
 * compartan bitmap; cambiar colores, estado, forma o tamano genera uno nuevo.
 */
data class BusSpriteKey(
    val body: Int,
    val accent: Int,
    val ring: Int,
    val state: BusMapState,
    val icon: BusIcon,
    val sizePx: Int,
)

/** Cache LRU pequena de sprites ya dibujados: se dibuja una vez por [BusSpriteKey], nunca por frame. */
object BusSpriteCache {
    private const val MAX = 16
    private val map = LinkedHashMap<BusSpriteKey, ImageBitmap>()

    fun get(key: BusSpriteKey): ImageBitmap {
        map.remove(key)?.let { map[key] = it; return it }
        val bmp = renderBusSprite(key)
        map[key] = bmp
        if (map.size > MAX) map.remove(map.keys.first())
        return bmp
    }

    fun clear() = map.clear()
}

private fun darken(c: Color, f: Float) = lerp(c, Color.Black, f)

/**
 * Dibuja el bus apuntando hacia arriba (norte) en un lienzo cuadrado de [BusSpriteKey.sizePx].
 * El anillo exterior codifica el estado; la rotacion por rumbo la aplica el mapa.
 */
fun renderBusSprite(key: BusSpriteKey): ImageBitmap {
    val s = key.sizePx
    val image = ImageBitmap(s, s)
    val canvas = Canvas(image)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(s.toFloat(), s.toFloat())) {
        drawBus(key)
    }
    return image
}

private fun DrawScope.drawBus(key: BusSpriteKey) {
    val u = size.minDimension / 96f
    val c = Offset(size.width / 2f, size.height / 2f)
    val body = Color(key.body)
    val accent = Color(key.accent)
    val ring = Color(key.ring)
    val glass = darken(body, 0.62f)

    // Halo y anillo de estado
    drawCircle(Color.White.copy(alpha = 0.92f), radius = 45f * u, center = c)
    drawCircle(ring, radius = 44f * u, center = c, style = Stroke(width = 5f * u))
    if (key.state == BusMapState.Arriving) {
        drawCircle(ring.copy(alpha = 0.45f), radius = 38.5f * u, center = c, style = Stroke(width = 2f * u))
    }

    val (w, h, r) = when (key.icon) {
        BusIcon.Classic -> Triple(34f, 66f, 9f)
        BusIcon.Modern -> Triple(32f, 70f, 15f)
        BusIcon.Minibus -> Triple(30f, 52f, 11f)
    }
    val bw = w * u
    val bh = h * u
    val left = c.x - bw / 2f
    val top = c.y - bh / 2f

    // Sombra suave
    drawRoundRect(Color.Black.copy(alpha = 0.18f), Offset(left + 1.5f * u, top + 2f * u), Size(bw, bh), CornerRadius(r * u))
    // Carroceria
    drawRoundRect(body, Offset(left, top), Size(bw, bh), CornerRadius(r * u))
    // Franja de acento
    drawRect(accent, Offset(left, top + bh * 0.58f), Size(bw, 5f * u))
    // Parabrisas (frente = arriba)
    val wsH = bh * 0.17f
    drawRoundRect(glass, Offset(left + 3f * u, top + 4f * u), Size(bw - 6f * u, wsH), CornerRadius(5f * u))
    // Ventanas laterales
    val winCount = if (key.icon == BusIcon.Minibus) 2 else 3
    val winArea = bh * 0.45f
    val winH = winArea / winCount - 3f * u
    for (i in 0 until winCount) {
        val y = top + bh * 0.28f + i * (winArea / winCount)
        drawRoundRect(glass, Offset(left + 1.5f * u, y), Size(3f * u, winH), CornerRadius(1.5f * u))
        drawRoundRect(glass, Offset(left + bw - 4.5f * u, y), Size(3f * u, winH), CornerRadius(1.5f * u))
    }
    // Techo con claraboya
    drawRoundRect(darken(body, 0.12f), Offset(left + 8f * u, top + bh * 0.30f), Size(bw - 16f * u, bh * 0.22f), CornerRadius(3f * u))
    // Faros
    drawCircle(Color(0xFFFFF3C4), radius = 2.2f * u, center = Offset(left + 7f * u, top + 2.4f * u))
    drawCircle(Color(0xFFFFF3C4), radius = 2.2f * u, center = Offset(left + bw - 7f * u, top + 2.4f * u))
    // Luces traseras
    drawCircle(Color(0xFFFF5A5A), radius = 1.8f * u, center = Offset(left + 7f * u, top + bh - 2.2f * u))
    drawCircle(Color(0xFFFF5A5A), radius = 1.8f * u, center = Offset(left + bw - 7f * u, top + bh - 2.2f * u))
}

/** Convierte un [Color] a ARGB entero para usar como clave. */
fun Color.toKeyArgb(): Int = toArgb()
