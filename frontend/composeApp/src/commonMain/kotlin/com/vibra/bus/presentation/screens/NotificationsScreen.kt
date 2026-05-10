package com.vibra.bus.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import com.vibra.bus.data.model.NotificationItem
import com.vibra.bus.presentation.components.EmptyState
import com.vibra.bus.presentation.viewmodel.NotificationsViewModel
import com.vibra.bus.util.toRelativeTime
import org.koin.compose.viewmodel.koinViewModel
import vibrabus.composeapp.generated.resources.Res
import vibrabus.composeapp.generated.resources.buho_con_celular

class NotificationsScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel     = koinViewModel<NotificationsViewModel>()
        val notifications by viewModel.notifications.collectAsState()

        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {

                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Text(
                        text       = "Notificaciones",
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onPrimary,
                        modifier   = Modifier.align(Alignment.CenterStart),
                    )
                    if (notifications.isNotEmpty()) {
                        IconButton(
                            onClick  = { viewModel.clearAll() },
                            modifier = Modifier.align(Alignment.CenterEnd),
                        ) {
                            Icon(
                                imageVector        = Icons.Default.DeleteSweep,
                                contentDescription = "Borrar todo",
                                tint               = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            )
                        }
                    }
                }

                if (notifications.isEmpty()) {
                    EmptyState(
                        message  = "Sin notificaciones",
                        subtitle = "Aquí verás las alertas de tu bus",
                        image    = Res.drawable.buho_con_celular,
                    )
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        items(notifications, key = { it.id }) { notif ->
                            SwipeToDeleteNotification(
                                notif    = notif,
                                onDelete = { viewModel.deleteNotification(notif.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteNotification(notif: NotificationItem, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 5.dp)
                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(14.dp))
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector        = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint               = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        modifier = Modifier.padding(vertical = 5.dp),
    ) {
        NotificationCard(notif)
    }
}

@Composable
private fun NotificationCard(notif: NotificationItem) {
    val (icon, color) = when (notif.type) {
        "bus_arrival"     -> Pair("🚌", MaterialTheme.colorScheme.primary)
        "bus_approaching" -> Pair("⏰", MaterialTheme.colorScheme.secondary)
        "bus_almost_full" -> Pair("⚠️", Color(0xFFF97316))
        else              -> Pair("🔔", MaterialTheme.colorScheme.primary)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)),
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier         = Modifier
                    .size(42.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(icon, fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(notif.title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                Text(notif.body,  color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            Text(notif.timestamp.toRelativeTime(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}
