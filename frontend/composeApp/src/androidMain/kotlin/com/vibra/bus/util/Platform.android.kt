package com.vibra.bus.util

import android.os.Build
import android.util.Log
import com.vibra.bus.BuildConfig

actual val DEVICE_PLATFORM: String = "android"

actual val DEVICE_NAME: String = sanitizeDeviceName("${Build.MANUFACTURER} ${Build.MODEL}", "Android")

/** Client id de servidor valido: no vacio y con la forma de un id de OAuth de Google (no un placeholder). */
actual val isGoogleSignInAvailable: Boolean = run {
    val id = BuildConfig.GOOGLE_SERVER_CLIENT_ID
    val ok = id.isNotBlank() && id.endsWith(".apps.googleusercontent.com") && !id.contains("YOUR", ignoreCase = true)
    if (!ok && BuildConfig.DEBUG) {
        Log.w("GoogleSignIn", "GOOGLE_SERVER_CLIENT_ID vacio o placeholder: se oculta el boton de Google")
    }
    ok
}
