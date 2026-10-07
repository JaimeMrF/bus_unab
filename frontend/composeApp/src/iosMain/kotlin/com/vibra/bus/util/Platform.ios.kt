package com.vibra.bus.util

import platform.UIKit.UIDevice

actual val DEVICE_PLATFORM: String = "ios"

actual val DEVICE_NAME: String = sanitizeDeviceName(UIDevice.currentDevice.name, "iPhone")

/** El SDK de Google Sign-In para iOS aun no esta integrado: el boton se oculta y se usa correo y contrasena. */
actual val isGoogleSignInAvailable: Boolean = false
