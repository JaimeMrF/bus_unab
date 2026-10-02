package com.vibra.bus.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.vibra.bus.presentation.theme.Elevation

data class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
)

@Composable
fun BottomNavBar(
    items: List<NavItem>,
    selectedRoute: String,
    onTabSelected: (NavItem) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = Elevation.low,
    ) {
        items.forEach { item ->
            val selected = item.route == selectedRoute
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(item) },
                icon = { Icon(item.icon, contentDescription = null) },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

fun studentNavItems(): List<NavItem> = listOf(
    NavItem("Inicio",     Icons.Default.Home,          "home"),
    NavItem("Mis Viajes", Icons.Default.DirectionsBus, "trips"),
    NavItem("Wallet",     Icons.Default.Wallet,        "qr"),
    NavItem("Alertas",    Icons.Default.Notifications, "notifications"),
    NavItem("Perfil",     Icons.Default.Person,        "profile"),
)

fun driverNavItems(): List<NavItem> = listOf(
    NavItem("Inicio",    Icons.Default.Home,          "home"),
    NavItem("Mis Rutas", Icons.Default.DirectionsBus, "trips"),
    NavItem("Escanear",  Icons.Default.CameraAlt,     "scanner"),
    NavItem("Alertas",   Icons.Default.Notifications, "notifications"),
    NavItem("Perfil",    Icons.Default.Person,        "profile"),
)
