package com.vibra.bus.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibra.bus.presentation.theme.AppColors

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
        containerColor = AppColors.White,
        contentColor   = AppColors.PrimaryPurple,
        tonalElevation = 0.dp,
    ) {
        items.forEach { item ->
            val selected = item.route == selectedRoute
            NavigationBarItem(
                selected  = selected,
                onClick   = { onTabSelected(item) },
                icon      = {
                    Icon(
                        imageVector     = item.icon,
                        contentDescription = item.label,
                        modifier        = Modifier.size(22.dp),
                    )
                },
                label     = {
                    Text(
                        text     = item.label,
                        fontSize = 10.sp,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = AppColors.PrimaryPurple,
                    selectedTextColor   = AppColors.PrimaryPurple,
                    unselectedIconColor = AppColors.TextSecondary,
                    unselectedTextColor = AppColors.TextSecondary,
                    indicatorColor      = AppColors.PrimaryPurple.copy(alpha = 0.10f),
                ),
            )
        }
    }
}

fun studentNavItems(): List<NavItem> = listOf(
    NavItem("Inicio",     Icons.Default.Home,          "home"),
    NavItem("Mis Viajes", Icons.Default.DirectionsBus, "trips"),
    NavItem("QR",         Icons.Default.QrCode,        "qr"),
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
