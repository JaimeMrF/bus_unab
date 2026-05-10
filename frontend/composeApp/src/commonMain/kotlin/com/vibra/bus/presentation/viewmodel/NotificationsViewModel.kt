package com.vibra.bus.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.vibra.bus.data.model.NotificationItem
import com.vibra.bus.util.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class NotificationsViewModel(private val settings: AppSettings) : ViewModel() {

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications

    init {
        loadNotifications()
        markAllRead()
    }

    private fun loadNotifications() {
        try {
            val json = settings.notificationsJson
            _notifications.value = Json.decodeFromString<List<NotificationItem>>(json)
        } catch (e: Exception) {
            _notifications.value = emptyList()
        }
    }

    private fun markAllRead() {
        val updated = _notifications.value.map { it.copy(isRead = true) }
        _notifications.value = updated
        settings.notificationsJson = Json.encodeToString(updated)
    }

    fun addNotification(item: NotificationItem) {
        val updated = listOf(item) + _notifications.value
        _notifications.value = updated
        settings.notificationsJson = Json.encodeToString(updated)
    }

    fun deleteNotification(id: String) {
        val updated = _notifications.value.filter { it.id != id }
        _notifications.value = updated
        settings.notificationsJson = Json.encodeToString(updated)
    }

    fun clearAll() {
        _notifications.value = emptyList()
        settings.notificationsJson = Json.encodeToString(emptyList<NotificationItem>())
    }
}
