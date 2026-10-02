package com.vibra.bus.presentation.screens

import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.DirectionsBus
import com.vibra.bus.presentation.components.AppCard
import com.vibra.bus.presentation.theme.AppShape
import androidx.compose.material.icons.filled.NotificationsNone
import com.vibra.bus.presentation.components.AppTopBar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.data.model.NotificationItem
import com.vibra.bus.presentation.components.EmptyState
import com.vibra.bus.presentation.viewmodel.NotificationsViewModel
import com.vibra.bus.util.toRelativeTime
import org.koin.compose.viewmodel.koinViewModel
import vibrabus.composeapp.generated.resources.Res

class NotificationsScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator     = LocalNavigator.currentOrThrow
        val viewModel     = koinViewModel<NotificationsViewModel>()
        val notifications by viewModel.notifications.collectAsState()

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                AppTopBar(
                    title = "Notificaciones",
                    actions = {
                        if (notifications.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearAll() }) {
                                Icon(
                                    Icons.Default.DeleteSweep,
                                    contentDescription = "Borrar todas las notificaciones",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                )
            },
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (notifications.isEmpty()) {
                    EmptyState(
                        message  = "Sin notificaciones",
                        subtitle = "Aquí verás las alertas de tu bus",
                        icon     = Icons.Default.NotificationsNone,
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
                    .background(MaterialTheme.colorScheme.errorContainer, AppShape.Card)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector        = Icons.Default.Delete,
                    contentDescription = null,
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
    val scheme = MaterialTheme.colorScheme
    val (icon, container, content) = when (notif.type) {
        "bus_arrival"     -> Triple(Icons.Default.DirectionsBus, scheme.primaryContainer, scheme.onPrimaryContainer)
        "bus_approaching" -> Triple(Icons.Default.Schedule, scheme.secondaryContainer, scheme.onSecondaryContainer)
        "bus_almost_full" -> Triple(Icons.Default.Warning, scheme.errorContainer, scheme.onErrorContainer)
        else              -> Triple(Icons.Default.Notifications, scheme.surfaceVariant, scheme.onSurfaceVariant)
    }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier         = Modifier.size(42.dp).background(container, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(notif.title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                Text(notif.body, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Text(notif.timestamp.toRelativeTime(), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        }
    }
}
