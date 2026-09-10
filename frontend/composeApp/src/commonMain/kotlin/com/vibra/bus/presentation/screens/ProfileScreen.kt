package com.vibra.bus.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.vibra.bus.util.AppSettings
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

class ProfileScreen : Screen {

    @Composable
    override fun Content() {
        val navigator     = LocalNavigator.currentOrThrow
        val viewModel     = koinViewModel<ProfileViewModel>()
        val authViewModel = koinViewModel<AuthViewModel>()
        val settings      = koinInject<AppSettings>()
        val profile       by viewModel.profile.collectAsState()
        val authEvent     by authViewModel.event.collectAsState()
        var showLogout    by remember { mutableStateOf(false) }
        val isDark        by settings.isDarkThemeFlow.collectAsState()

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
                        Text("Sí, cerrar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton    = {
                    TextButton(onClick = { showLogout = false }) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.primary)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
            )
        }

        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            ) {

                // ── Header con gradiente ──────────────────────────────────────
                val headerGradient = if (isDark)
                    Brush.verticalGradient(listOf(Color(0xFF2D1050), Color(0xFF5B2C8C)))
                else
                    Brush.verticalGradient(listOf(Color(0xFFCC8500), Color(0xFFE9A427)))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerGradient)
                        .padding(top = 36.dp, bottom = 28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Avatar
                        if (profile.avatar.isNotEmpty()) {
                            AsyncImage(
                                model              = profile.avatar,
                                contentDescription = "Avatar",
                                modifier           = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .border(3.dp, MaterialTheme.colorScheme.secondary, CircleShape),
                            )
                        } else {
                            val avatarGradient = if (isDark)
                                Brush.radialGradient(listOf(Color(0xFF7B4DB0), Color(0xFF3A1A68)))
                            else
                                Brush.radialGradient(listOf(Color(0xFFE9A427), Color(0xFFCC8500)))
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(avatarGradient)
                                    .border(3.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text       = profile.name.firstOrNull()?.uppercase() ?: "U",
                                    fontSize   = 40.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color.White,
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text       = profile.name,
                            fontSize   = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text  = profile.email,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.65f),
                        )
                        Spacer(Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                    RoundedCornerShape(20.dp)
                                )
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 18.dp, vertical = 5.dp),
                        ) {
                            Text(
                                text       = if (profile.role == "driver") "Conductor" else "Pasajero",
                                color      = MaterialTheme.colorScheme.secondary,
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                // ── Franja de info rápida ─────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    QuickStat(value = "BGA", label = "Ciudad")
                    Box(
                        Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    QuickStat(
                        value = if (profile.role == "driver") "Conductor" else "Pasajero",
                        label = "Rol"
                    )
                    Box(
                        Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    QuickStat(value = "Activo", label = "Estado")
                }

                Spacer(Modifier.height(10.dp))

                // ── Opciones ──────────────────────────────────────────────────
                ProfileSectionLabel("Opciones")

                ProfileItem(
                    icon     = Icons.Filled.Notifications,
                    label    = "Notificaciones",
                    subtitle = "Alertas y avisos de tu bus",
                    iconBg   = Color(0xFF155A38),
                    onClick  = { navigator.push(NotificationsScreen()) },
                )
                ProfileItem(
                    icon     = Icons.Filled.History,
                    label    = "Mis viajes",
                    subtitle = "Historial de trayectos",
                    iconBg   = Color(0xFF0C4F7A),
                    onClick  = { navigator.push(MyTripsScreen()) },
                )
                ProfileItem(
                    icon     = Icons.Filled.QrCode,
                    label    = "Mi código QR",
                    subtitle = "Acceso rápido al código de viaje",
                    iconBg   = Color(0xFF6B4000),
                    onClick  = { navigator.push(MyQRScreen()) },
                )

                Spacer(Modifier.height(10.dp))

                // ── Apariencia ────────────────────────────────────────
                ProfileSectionLabel("Apariencia")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 3.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Palette,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = "Tema de color",
                            color      = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            fontSize   = 15.sp,
                        )
                        Text(
                            text     = if (isDark) "Morado (oscuro)" else "Naranja (cálido)",
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                        )
                    }
                    Switch(
                        checked         = !isDark,
                        onCheckedChange = { checked -> settings.isDarkTheme = !checked },
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ── Cerrar sesión ─────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                ) {
                    Button(
                        onClick  = { showLogout = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                        ),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Default.ExitToApp,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Cerrar sesión",
                            color      = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Spacer(Modifier.height(22.dp))

                    Column(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Bucaramanga Mobility v1.0.0", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Universidad Autónoma de Bucaramanga",
                            fontSize = 11.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Spacer(Modifier.height(3.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
    }
}

@Composable
private fun ProfileSectionLabel(title: String) {
    Text(
        text          = title.uppercase(),
        fontSize      = 11.sp,
        fontWeight    = FontWeight.SemiBold,
        color         = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.1.sp,
        modifier      = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

@Composable
private fun ProfileItem(
    icon    : ImageVector,
    label   : String,
    subtitle: String,
    iconBg  : Color,
    onClick : () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint               = Color.White,
                modifier           = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = label,
                color      = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                fontSize   = 15.sp,
            )
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = subtitle,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Default.ArrowForwardIos,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier           = Modifier.size(13.dp),
        )
    }
}
