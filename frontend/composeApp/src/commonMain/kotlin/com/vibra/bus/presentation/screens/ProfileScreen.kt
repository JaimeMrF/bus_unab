package com.vibra.bus.presentation.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.vibra.bus.presentation.theme.AppColors
import com.vibra.bus.presentation.viewmodel.AuthEvent
import com.vibra.bus.presentation.viewmodel.AuthViewModel
import com.vibra.bus.presentation.viewmodel.ProfileViewModel
import org.koin.compose.viewmodel.koinViewModel

class ProfileScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<ProfileViewModel>()
        val authViewModel = koinViewModel<AuthViewModel>()
        val profile by viewModel.profile.collectAsState()
        val authEvent by authViewModel.event.collectAsState()
        var showLogoutDialog by remember { mutableStateOf(false) }

        LaunchedEffect(authEvent) {
            if (authEvent is AuthEvent.NavigateToLogin) {
                val rootNavigator = navigator.parent ?: navigator
                rootNavigator.replaceAll(LoginScreen())
                authViewModel.consumeEvent()
            }
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Cerrar sesión") },
                text = { Text("¿Estás seguro de que deseas cerrar sesión?") },
                confirmButton = {
                    TextButton(onClick = {
                        showLogoutDialog = false
                        authViewModel.logout()
                    }) { Text("Sí, cerrar sesión", color = AppColors.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar") }
                },
            )
        }

        Scaffold(containerColor = AppColors.PrimaryBg) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(24.dp))
                if (profile.avatar.isNotEmpty()) {
                    AsyncImage(
                        model = profile.avatar,
                        contentDescription = "Avatar",
                        modifier = Modifier.size(88.dp).clip(CircleShape),
                    )
                } else {
                    Box(
                        modifier = Modifier.size(88.dp).background(AppColors.MediumPurple, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = profile.name.firstOrNull()?.uppercase() ?: "U",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.White,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(profile.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColors.White)
                Text(profile.email, fontSize = 13.sp, color = AppColors.GrayText)
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.background(AppColors.MediumPurple, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (profile.role == "driver") "Conductor" else "Estudiante",
                        color = AppColors.White,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.height(32.dp))

                if (viewModel.isDriver()) {
                    ProfileOption(label = "Modo Conductor") { navigator.push(DriverModeScreen()) }
                }
                ProfileOption(label = "Recargar saldo") { navigator.push(RechargeScreen()) }
                ProfileOption(label = "Notificaciones") { navigator.push(NotificationsScreen()) }

                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Red),
                ) {
                    Icon(Icons.AutoMirrored.Default.ExitToApp, contentDescription = null, tint = AppColors.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Cerrar sesión", color = AppColors.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ProfileOption(label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.DarkHeader),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = AppColors.White, modifier = Modifier.weight(1f), fontSize = 15.sp)
            Icon(
                Icons.AutoMirrored.Default.ArrowForwardIos,
                contentDescription = null,
                tint = AppColors.GrayText,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
