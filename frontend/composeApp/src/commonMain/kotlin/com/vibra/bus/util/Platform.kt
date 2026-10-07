package com.vibra.bus.util

expect val DEVICE_PLATFORM: String

/** Nombre legible del dispositivo para el login (maximo 60 caracteres). */
expect val DEVICE_NAME: String

/** false si no hay forma de iniciar sesion con Google en esta build (iOS sin SDK, client id ausente). */
expect val isGoogleSignInAvailable: Boolean

/** Recorta y limpia un nombre de dispositivo al maximo que acepta el backend. */
fun sanitizeDeviceName(raw: String, fallback: String): String =
    raw.trim().replace(Regex("\\s+"), " ").take(60).ifBlank { fallback }
