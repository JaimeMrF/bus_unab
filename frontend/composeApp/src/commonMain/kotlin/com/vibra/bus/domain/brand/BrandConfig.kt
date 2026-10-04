package com.vibra.bus.domain.brand

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Paleta de un modo (claro u oscuro) tal como la entrega la API: hex #RRGGBB. */
@Serializable
data class BrandPalette(
    val primary: String,
    @SerialName("on_primary") val onPrimary: String,
    val secondary: String,
    @SerialName("on_secondary") val onSecondary: String,
    val background: String,
    val surface: String,
    @SerialName("on_surface") val onSurface: String,
    val accent: String,
    val success: String,
    val warning: String,
    val error: String,
)

@Serializable
data class BrandColors(val light: BrandPalette, val dark: BrandPalette)

@Serializable
data class BrandFeatures(
    @SerialName("qr_payments") val qrPayments: Boolean = true,
    val wallet: Boolean = true,
    @SerialName("driver_mode") val driverMode: Boolean = true,
)

/** Poses de la mascota (URLs https o null). Todas opcionales: el cliente degrada a la mascota única o a nada. */
@Serializable
data class MascotPoses(
    val greeting: String? = null,
    val curious: String? = null,
    val sad: String? = null,
    val waiting: String? = null,
    val phone: String? = null,
    val map: String? = null,
    val driver: String? = null,
    val ok: String? = null,
    val celebrating: String? = null,
)

/** Estilo del bus en el mapa. body y accent null: se usan primary y secondary del tenant. */
@Serializable
data class BusStyle(
    val body: String? = null,
    val accent: String? = null,
    val icon: String? = "classic",
    @SerialName("icon_url") val iconUrl: String? = null,
)

/** Perfil de marca de una organización (tenant). Contrato: GET /branding/{slug}. */
@Serializable
data class BrandConfig(
    val slug: String,
    @SerialName("app_name") val appName: String,
    val tagline: String? = null,
    @SerialName("support_email") val supportEmail: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("logo_dark_url") val logoDarkUrl: String? = null,
    @SerialName("icon_url") val iconUrl: String? = null,
    @SerialName("mascot_url") val mascotUrl: String? = null,
    @SerialName("mascot_poses") val mascotPoses: MascotPoses? = null,
    @SerialName("bus_style") val busStyle: BusStyle? = null,
    val colors: BrandColors,
    @SerialName("font_family") val fontFamily: String = "system",
    @SerialName("corner_radius") val cornerRadius: String = "md",
    val features: BrandFeatures = BrandFeatures(),
    val version: Int = 0,
) {
    /** URL de la pose pedida; si falta usa la mascota única; si tampoco hay, null (layout sin hueco). */
    fun poseUrl(pose: MascotPose): String? {
        val p = mascotPoses
        val url = when (pose) {
            MascotPose.Greeting -> p?.greeting
            MascotPose.Curious -> p?.curious
            MascotPose.Sad -> p?.sad
            MascotPose.Waiting -> p?.waiting
            MascotPose.Phone -> p?.phone
            MascotPose.Map -> p?.map
            MascotPose.Driver -> p?.driver
            MascotPose.Ok -> p?.ok
            MascotPose.Celebrating -> p?.celebrating
        }
        return (url ?: mascotUrl)?.takeIf { it.isNotBlank() }
    }

    fun logoFor(dark: Boolean): String? = if (dark) logoDarkUrl ?: logoUrl else logoUrl

    companion object {
        /** Tema neutro embebido: sin marca. Se usa mientras no hay organización o si todo falla. */
        val Neutral = BrandConfig(
            slug = "",
            appName = "Transporte",
            fontFamily = "poppins", // fuente embebida: el tema neutro no depende de la fuente del sistema
            tagline = null,
            colors = BrandColors(
                light = BrandPalette(
                    primary = "#2F4B7C", onPrimary = "#FFFFFF",
                    secondary = "#5B6B85", onSecondary = "#FFFFFF",
                    background = "#F6F7FA", surface = "#FFFFFF", onSurface = "#12161D",
                    accent = "#0E8F8A", success = "#1E8E55", warning = "#B26A00", error = "#BA1A1A",
                ),
                dark = BrandPalette(
                    primary = "#9DB6EA", onPrimary = "#0B1B3A",
                    secondary = "#B4C0D6", onSecondary = "#15202F",
                    background = "#0D1117", surface = "#161B24", onSurface = "#E7EAF0",
                    accent = "#4FD1C5", success = "#3CCB7F", warning = "#F2B14C", error = "#FF6B6B",
                ),
            ),
            version = 0,
        )

        /** Marca de demostración usada para mockear la API mientras el backend no esté listo. */
        val Demo = Neutral.copy(
            slug = "demo",
            appName = "Demo Transit",
            tagline = "Muévete mejor",
            colors = BrandColors(
                light = Neutral.colors.light.copy(primary = "#0B5FA5", accent = "#E4572E"),
                dark = Neutral.colors.dark.copy(primary = "#7DB8F0", onPrimary = "#06223D", accent = "#FF8A65"),
            ),
            version = 1,
        )
    }
}

enum class MascotPose { Greeting, Curious, Sad, Waiting, Phone, Map, Driver, Ok, Celebrating }

/** "#RRGGBB" -> Color; valor inválido -> [fallback] (la API ya valida, esto es defensa en cliente). */
fun parseHexColor(hex: String, fallback: Color): Color {
    val s = hex.removePrefix("#")
    if (s.length != 6 || !s.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return fallback
    val rgb = s.toLong(16)
    return Color((0xFF000000L or rgb).toInt())
}
