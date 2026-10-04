package com.vibra.bus.util

import android.content.Intent
import com.vibra.bus.NotificationHelper

actual fun shareText(text: String) {
    val context = getPlatformContext()
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

actual fun showLocalNotification(title: String, body: String) {
    val context = getPlatformContext()
    NotificationHelper.createChannels(context)
    NotificationHelper.show(context, "bus_approaching", title, body, mapOf("minutes_away" to "0"))
}
