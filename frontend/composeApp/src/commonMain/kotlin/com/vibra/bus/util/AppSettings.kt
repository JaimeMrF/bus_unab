package com.vibra.bus.util

import com.russhwolf.settings.Settings

class AppSettings(private val settings: Settings) {

    var token: String
        get() = settings.getString("token", "")
        set(value) = settings.putString("token", value)

    var userRole: String
        get() = settings.getString("user_role", "student")
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

    var driverActivePlate: String
        get() = settings.getString("driver_active_plate", "")
        set(value) = settings.putString("driver_active_plate", value)

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
    }
}
