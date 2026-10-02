package com.vibra.bus.presentation.security

import androidx.compose.runtime.Composable

/**
 * Mientras esta funcion este en composicion: pantalla al brillo maximo, sin apagarse y, en
 * Android, protegida contra capturas y grabacion (FLAG_SECURE). En iOS no existe un bloqueo de
 * capturas equivalente: solo se sube el brillo y se evita el apagado. Se restaura al salir.
 */
@Composable
expect fun ProtectedQrDisplay()
