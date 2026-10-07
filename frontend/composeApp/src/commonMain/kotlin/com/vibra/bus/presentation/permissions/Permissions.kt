package com.vibra.bus.presentation.permissions

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/** Permisos que la app pide en contexto (nunca al abrir). La camara la gestiona el propio escaner. */
enum class AppPermission { Location, Notifications }

interface PermissionState {
    val granted: Boolean

    /** El usuario lo nego y el sistema ya no vuelve a preguntar: solo se puede activar en Ajustes. */
    val permanentlyDenied: Boolean

    fun request()
    fun openSettings()
}

@Composable
expect fun rememberPermissionState(permission: AppPermission): PermissionState

/**
 * Pantalla previa que explica para que se necesita el permiso antes de que el sistema lo pida.
 * [onDismiss] corresponde a "Ahora no": la funcion sigue disponible sin el permiso.
 */
@Composable
fun PermissionRationaleDialog(
    title: String,
    message: String,
    state: PermissionState,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = {
                    if (state.permanentlyDenied) state.openSettings() else state.request()
                    onDismiss()
                },
            ) { Text(if (state.permanentlyDenied) "Abrir ajustes" else "Permitir") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Ahora no") } },
    )
}
