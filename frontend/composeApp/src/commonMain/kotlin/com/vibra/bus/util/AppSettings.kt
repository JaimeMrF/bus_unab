package com.vibra.bus.util

import com.vibra.bus.data.api.ServerConfig
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettings(private val settings: Settings) {

    /** Preferencia de apariencia: "system" (por defecto), "light" o "dark". */
    private val _themeModeFlow = MutableStateFlow(settings.getString("theme_mode", "system"))
    val themeModeFlow: StateFlow<String> = _themeModeFlow.asStateFlow()

    private val _hasActiveTrackingFlow = MutableStateFlow(settings.getString("tracking_plate", "").isNotEmpty())
    val hasActiveTrackingFlow: StateFlow<Boolean> = _hasActiveTrackingFlow.asStateFlow()

    /**
     * Servidor de la API elegido por el usuario (ya normalizado, termina en /api/v1).
     * Vacío = se usa la URL del build. Se propaga a [ServerConfig] para todas las peticiones.
     */
    var serverUrl: String
        get() = settings.getString("server_url", "")
        set(value) {
            settings.putString("server_url", value)
            ServerConfig.override = value
        }

    init {
        ServerConfig.override = settings.getString("server_url", "")
    }

    /** Organización (tenant) elegida y su último BrandConfig serializado, para arranque offline. */
    var orgSlug: String
        get() = settings.getString("org_slug", "")
        set(value) = settings.putString("org_slug", value)

    var brandJson: String
        get() = settings.getString("brand_json", "")
        set(value) = settings.putString("brand_json", value)

    var token: String
        get() = settings.getString("token", "")
        set(value) = settings.putString("token", value)

    var userRole: String
        get() = settings.getString("user_role", "pasajero")
        set(value) = settings.putString("user_role", value)

    var userId: Int
        get() = settings.getInt("user_id", -1)
        set(value) = settings.putInt("user_id", value)

    var userName: String
        get() = settings.getString("user_name", "")
        set(value) = settings.putString("user_name", value)

    var userEmail: String
        get() = settings.getString("user_email", "")
        set(value) = settings.putString("user_email", value)

    var userAvatar: String
        get() = settings.getString("user_avatar", "")
        set(value) = settings.putString("user_avatar", value)

    var activeRequestId: Int
        get() = settings.getInt("active_request_id", -1)
        set(value) = settings.putInt("active_request_id", value)

    var activeRequestBusId: Int
        get() = settings.getInt("active_request_bus_id", -1)
        set(value) = settings.putInt("active_request_bus_id", value)

    var activeRequestStopId: Int
        get() = settings.getInt("active_request_stop_id", -1)
        set(value) = settings.putInt("active_request_stop_id", value)

    var fcmToken: String
        get() = settings.getString("fcm_token", "")
        set(value) = settings.putString("fcm_token", value)

    var notificationsJson: String
        get() = settings.getString("notifications_json", "[]")
        set(value) = settings.putString("notifications_json", value)

    var themeMode: String
        get() = _themeModeFlow.value
        set(value) {
            settings.putString("theme_mode", value)
            _themeModeFlow.value = value
        }

    var driverActivePlate: String
        get() = settings.getString("driver_active_plate", "")
        set(value) = settings.putString("driver_active_plate", value)

    // ── Active bus tracking session ───────────────────────────────────────────

    var trackingPlate: String
        get() = settings.getString("tracking_plate", "")
        set(value) = settings.putString("tracking_plate", value)

    var trackingStopId: Int
        get() = settings.getInt("tracking_stop_id", -1)
        set(value) = settings.putInt("tracking_stop_id", value)

    var trackingStopName: String
        get() = settings.getString("tracking_stop_name", "")
        set(value) = settings.putString("tracking_stop_name", value)

    var trackingStopAddress: String
        get() = settings.getString("tracking_stop_address", "")
        set(value) = settings.putString("tracking_stop_address", value)

    var trackingStopLat: Double
        get() = settings.getDouble("tracking_stop_lat", 0.0)
        set(value) = settings.putDouble("tracking_stop_lat", value)

    var trackingStopLng: Double
        get() = settings.getDouble("tracking_stop_lng", 0.0)
        set(value) = settings.putDouble("tracking_stop_lng", value)

    fun hasActiveTracking(): Boolean = trackingPlate.isNotEmpty()

    fun saveTracking(plate: String, stopId: Int, stopName: String, stopAddress: String, stopLat: Double, stopLng: Double) {
        trackingPlate       = plate
        trackingStopId      = stopId
        trackingStopName    = stopName
        trackingStopAddress = stopAddress
        trackingStopLat     = stopLat
        trackingStopLng     = stopLng
        _hasActiveTrackingFlow.value = true
    }

    fun clearTracking() {
        settings.remove("tracking_plate")
        settings.remove("tracking_stop_id")
        settings.remove("tracking_stop_name")
        settings.remove("tracking_stop_address")
        settings.remove("tracking_stop_lat")
        settings.remove("tracking_stop_lng")
        _hasActiveTrackingFlow.value = false
    }

    var batteryPromptShown: Boolean
        get() = settings.getBoolean("battery_prompt_shown", false)
        set(value) = settings.putBoolean("battery_prompt_shown", value)

    fun isLoggedIn(): Boolean = token.isNotEmpty()

    fun clearSession() {
        settings.remove("token")
        settings.remove("user_role")
        settings.remove("user_id")
        settings.remove("user_name")
        settings.remove("user_email")
        settings.remove("user_avatar")
        settings.remove("active_request_id")
        settings.remove("active_request_bus_id")
        settings.remove("active_request_stop_id")
        settings.remove("driver_active_plate")
        clearTracking()
    }
}
