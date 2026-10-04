package com.vibra.bus.util

/** Abre la hoja nativa de compartir con un texto (viaje, ETA). */
expect fun shareText(text: String)

/** Notificacion local inmediata (p. ej. aviso de que el bus esta a X minutos). */
expect fun showLocalNotification(title: String, body: String)
