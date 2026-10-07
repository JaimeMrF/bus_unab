package com.vibra.bus.presentation.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** iOS: el sistema muestra su propio aviso al usar la API (CoreLocation, notificaciones); sin pantalla previa propia aun. */
@Composable
actual fun rememberPermissionState(permission: AppPermission): PermissionState = remember {
    object : PermissionState {
        override val granted: Boolean = true
        override val permanentlyDenied: Boolean = false
        override fun request() = Unit
        override fun openSettings() = Unit
    }
}
