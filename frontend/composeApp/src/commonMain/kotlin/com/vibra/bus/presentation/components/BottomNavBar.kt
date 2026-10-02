package com.vibra.bus.presentation.components

import com.vibra.bus.presentation.motion.MotionSpec
import com.vibra.bus.presentation.motion.LocalMotion
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.Alignment
import androidx.compose.material3.Surface
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.vibra.bus.presentation.theme.Elevation

data class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
)

/**
 * Barra inferior con un único indicador "píldora" que se desliza con resorte entre pestañas.
 * La posición animada se lee en drawBehind (sin recomponer); el icono seleccionado hace un
 * pequeño pop por graphicsLayer. Cada pestaña es seleccionable con rol Tab (≥72dp de alto).
 */
@Composable
fun BottomNavBar(
    items: List<NavItem>,
    selectedRoute: String,
    onTabSelected: (NavItem) -> Unit,
) {
    val env = LocalMotion.current
    val selectedIndex = items.indexOfFirst { it.route == selectedRoute }.coerceAtLeast(0)
    val position = animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = MotionSpec.springy(env),
        label = "nav_indicator",
    )
    val indicator = MaterialTheme.colorScheme.primaryContainer
    val count = items.size.coerceAtLeast(1)

    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = Elevation.low) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(72.dp)
                .drawBehind {
                    val itemW = size.width / count
                    val pillW = 64.dp.toPx()
                    val pillH = 32.dp.toPx()
                    val cx = (position.value + 0.5f) * itemW
                    drawRoundRect(
                        color = indicator,
                        topLeft = Offset(cx - pillW / 2f, 10.dp.toPx()),
                        size = Size(pillW, pillH),
                        cornerRadius = CornerRadius(pillH / 2f, pillH / 2f),
                    )
                },
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val pop = animateFloatAsState(
                    targetValue = if (selected) 1.12f else 1f,
                    animationSpec = MotionSpec.springy(env),
                    label = "nav_pop",
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onTabSelected(item) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier.height(32.dp).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.graphicsLayer {
                                val v = pop.value
                                scaleX = v
                                scaleY = v
                            },
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
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
