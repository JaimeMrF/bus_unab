package com.vibra.bus.util

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

actual fun shareText(text: String) {
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    root.presentViewController(UIActivityViewController(listOf(text), null), true, null)
}

/** iOS: sin permiso ni planificador de notificaciones locales todavia; el aviso se ve dentro de la app. */
actual fun showLocalNotification(title: String, body: String) = Unit
