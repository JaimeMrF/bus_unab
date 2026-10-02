package com.vibra.bus.presentation.screens

import com.vibra.bus.presentation.motion.parallaxCollapse
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.SubcomposeAsyncImage
import com.vibra.bus.presentation.components.AppCard
import com.vibra.bus.presentation.components.AppTopBar
import com.vibra.bus.presentation.components.PillTone
import com.vibra.bus.presentation.components.SecondaryButton
import com.vibra.bus.presentation.components.StatusPill
import com.vibra.bus.presentation.theme.LocalBrand
import com.vibra.bus.presentation.viewmodel.AuthEvent
import com.vibra.bus.presentation.viewmodel.AuthViewModel
import com.vibra.bus.presentation.viewmodel.ProfileViewModel
import com.vibra.bus.util.AppSettings
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

class ProfileScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator     = LocalNavigator.currentOrThrow
        val viewModel     = koinViewModel<ProfileViewModel>()
        val authViewModel = koinViewModel<AuthViewModel>()
        val settings      = koinInject<AppSettings>()
        val brand         = LocalBrand.current
        val profile       by viewModel.profile.collectAsState()
        val authEvent     by authViewModel.event.collectAsState()
        val themeMode     by settings.themeModeFlow.collectAsState()
        var showLogout    by remember { mutableStateOf(false) }
        val scroll        = rememberScrollState()

        LaunchedEffect(authEvent) {
            if (authEvent is AuthEvent.NavigateToLogin) {
                (navigator.parent ?: navigator).replaceAll(LoginScreen())
                authViewModel.consumeEvent()
            }
        }

        if (showLogout) {
            AlertDialog(
                onDismissRequest = { showLogout = false },
                title = { Text("Cerrar sesión") },
                text = { Text("¿Estás seguro de que deseas cerrar sesión?") },
                confirmButton = {
                    TextButton(onClick = { showLogout = false; authViewModel.logout() }) {
                        Text("Sí, cerrar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogout = false }) { Text("Cancelar") }
                },
                containerColor = MaterialTheme.colorScheme.surface,
            )
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            topBar = { AppTopBar(title = "Perfil") },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scroll)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ── Identidad ────────────────────────────────────────────────
                AppCard(modifier = Modifier.fillMaxWidth().parallaxCollapse({ scroll.value.toFloat() })) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(url = profile.avatar, name = profile.name)
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = profile.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            StatusPill(
                                text = if (profile.role == "driver") "Conductor" else "Pasajero",
                                tone = PillTone.Brand,
                            )
                        }
                    }
                }

                // ── Opciones ─────────────────────────────────────────────────
                SectionLabel("Opciones")
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    ProfileItem(
                        icon = Icons.Filled.Notifications,
                        label = "Notificaciones",
                        subtitle = "Alertas y avisos de tu bus",
                        onClick = { navigator.push(NotificationsScreen()) },
                    )
                    ProfileItem(
                        icon = Icons.Filled.History,
                        label = "Mis viajes",
                        subtitle = "Historial de trayectos",
                        onClick = { navigator.push(MyTripsScreen()) },
                    )
                    if (brand.features.qrPayments) {
                        ProfileItem(
                            icon = Icons.Filled.QrCode,
                            label = "Mi código QR",
                            subtitle = "Acceso rápido al código de viaje",
                            onClick = { navigator.push(MyQRScreen()) },
                        )
                    }
                }

                // ── Apariencia ───────────────────────────────────────────────
                SectionLabel("Apariencia")
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf("system" to "Sistema", "light" to "Claro", "dark" to "Oscuro")
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                    ) {
                        options.forEachIndexed { index, (key, label) ->
                            SegmentedButton(
                                selected = themeMode == key,
                                onClick = { settings.themeMode = key },
                                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) { Text(label) }
                        }
                    }
                }

                // ── Soporte ──────────────────────────────────────────────────
                brand.supportEmail?.takeIf { it.isNotBlank() }?.let { email ->
                    SectionLabel("Soporte")
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "¿Necesitas ayuda?",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                SecondaryButton(
                    text = "Cerrar sesión",
                    leadingIcon = Icons.AutoMirrored.Filled.ExitToApp,
                    onClick = { showLogout = true },
                )

                Text(
                    text = "${brand.appName} v1.0.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun Avatar(url: String, name: String) {
    val size = 64.dp
    val initial = @Composable {
        Box(
            modifier = Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "U",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
    if (url.isEmpty()) {
        initial()
    } else {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = "Foto de perfil",
            modifier = Modifier.size(size).clip(CircleShape),
            contentScale = ContentScale.Crop,
            loading = { initial() },
            error = { initial() },
        )
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp).semantics { heading() },
    )
}

@Composable
private fun ProfileItem(
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    androidx.compose.material3.Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
