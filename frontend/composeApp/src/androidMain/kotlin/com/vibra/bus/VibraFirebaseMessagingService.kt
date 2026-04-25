package com.vibra.bus

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.russhwolf.settings.Settings
import com.vibra.bus.data.model.NotificationItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

class VibraFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val settings = Settings()
        settings.putString("fcm_token", token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val type = data["type"] ?: "general"
        val title = notification?.title ?: data["title"] ?: "VIBRA+ Bus UNAB"
        val body = notification?.body ?: data["body"] ?: ""

        val item = NotificationItem(
            id = Random.nextInt().toString(),
            type = type,
            title = title,
            body = body,
            timestamp = System.currentTimeMillis(),
        )

        val settings = Settings()
        val existing = try {
            Json.decodeFromString<List<NotificationItem>>(settings.getString("notifications_json", "[]"))
        } catch (e: Exception) { emptyList() }

        val updated = listOf(item) + existing
        settings.putString("notifications_json", Json.encodeToString(updated))
    }
}
