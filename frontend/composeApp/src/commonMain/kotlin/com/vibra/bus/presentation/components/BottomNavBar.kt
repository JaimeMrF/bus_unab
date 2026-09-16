package com.vibra.bus.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(
            color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
            thickness = 0.5.dp,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val selected = item.route == selectedRoute
                val interactionSource = remember { MutableInteractionSource() }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = interactionSource,
                            indication        = null,
                            onClick           = { onTabSelected(item) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector        = item.icon,
                        contentDescription = item.label,
                        modifier           = Modifier.size(if (selected) 23.dp else 21.dp),
                        tint               = if (selected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (selected) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text       = item.label,
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
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
