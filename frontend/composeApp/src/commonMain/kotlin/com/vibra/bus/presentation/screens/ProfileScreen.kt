package com.vibra.bus.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.vibra.bus.presentation.viewmodel.AuthEvent
import com.vibra.bus.presentation.viewmodel.AuthViewModel
import com.vibra.bus.presentation.viewmodel.ProfileViewModel
import org.koin.compose.viewmodel.koinViewModel

class ProfileScreen : Screen {

    @Composable
    override fun Content() {
        val navigator    = LocalNavigator.currentOrThrow
        val viewModel    = koinViewModel<ProfileViewModel>()
        val authViewModel= koinViewModel<AuthViewModel>()
        val profile      by viewModel.profile.collectAsState()
        val authEvent    by authViewModel.event.collectAsState()
        var showLogout   by remember { mutableStateOf(false) }

        LaunchedEffect(authEvent) {
            if (authEvent is AuthEvent.NavigateToLogin) {
                (navigator.parent ?: navigator).replaceAll(LoginScreen())
                authViewModel.consumeEvent()
            }
        }

        if (showLogout) {
            AlertDialog(
                onDismissRequest = { showLogout = false },
                title            = { Text("Cerrar sesión", color = MaterialTheme.colorScheme.onSurface) },
                text             = { Text("¿Estás seguro de que deseas cerrar sesión?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                confirmButton    = {
                    TextButton(onClick = { showLogout = false; authViewModel.logout() }) {
                        Text("Sí, cerrar sesión", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogout = false }) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.primary)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
            )
        }

        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            Column(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Header morado
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Avatar
                        if (profile.avatar.isNotEmpty()) {
                            AsyncImage(
                                model           = profile.avatar,
                                contentDescription = "Avatar",
                                modifier        = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .border(3.dp, MaterialTheme.colorScheme.secondary, CircleShape),
                            )
                        } else {
                            Box(
                                modifier         = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.20f))
                                    .border(3.dp, MaterialTheme.colorScheme.secondary, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text       = profile.name.firstOrNull()?.uppercase() ?: "U",
                                    fontSize   = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = MaterialTheme.colorScheme.surface,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(profile.name,  fontSize = 20.sp, fontWeight = FontWeight.Bold,  color = MaterialTheme.colorScheme.surface)
                        Text(profile.email, fontSize = 13.sp, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                        Spacer(Modifier.height(8.dp))
                        // Badge dorado de rol
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text       = if (profile.role == "driver") "Conductor" else "Estudiante",
                                color      = MaterialTheme.colorScheme.secondary,
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                // Opciones
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                ) {
                    if (viewModel.isDriver()) {
                        ProfileOption(label = "Modo Conductor")   { navigator.push(DriverModeScreen()) }
                    }
                    ProfileOption(label = "Recargar saldo")       { navigator.push(RechargeScreen()) }
                    ProfileOption(label = "Notificaciones")       { navigator.push(NotificationsScreen()) }

                    Spacer(Modifier.height(32.dp))

                    // Botón cerrar sesión
                    Button(
                        onClick  = { showLogout = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(Icons.AutoMirrored.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.surface)
                        Spacer(Modifier.width(8.dp))
                        Text("Cerrar sesión", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileOption(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .shadow(2.dp, RoundedCornerShape(12.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), fontSize = 15.sp)
            Icon(
                Icons.AutoMirrored.Default.ArrowForwardIos,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
