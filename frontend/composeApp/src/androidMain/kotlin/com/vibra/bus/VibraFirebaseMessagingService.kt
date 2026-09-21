package com.vibra.bus

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.russhwolf.settings.Settings
import com.vibra.bus.data.model.NotificationItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

class VibraFirebaseMessagingService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        // Registrar los canales una sola vez al iniciar el servicio
        NotificationHelper.createChannels(this)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Settings().putString("fcm_token", token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data  = remoteMessage.data
        val notif = remoteMessage.notification

        val type  = data["type"]  ?: "general"
        val title = notif?.title  ?: data["title"] ?: "BUCARATRANSIT"
        val body  = notif?.body   ?: data["body"]  ?: ""

        // 1. Guardar en la lista local (visible en NotificationsScreen)
        saveToLocalList(type, title, body)

        // 2. Mostrar notificación del sistema (cuando la app está en primer plano
        //    FCM no la muestra automáticamente, lo hacemos nosotros aquí)
        NotificationHelper.show(this, type, title, body, data)
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun saveToLocalList(type: String, title: String, body: String) {
        val item = NotificationItem(
            id        = Random.nextInt().toString(),
            type      = type,
            title     = title,
            body      = body,
            timestamp = System.currentTimeMillis(),
        )
        val settings = Settings()
        val existing = try {
            Json.decodeFromString<List<NotificationItem>>(
                settings.getString("notifications_json", "[]")
            )
        } catch (_: Exception) { emptyList() }

        settings.putString("notifications_json", Json.encodeToString(listOf(item) + existing))
    }
}
