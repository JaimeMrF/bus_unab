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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.vibra.bus.presentation.theme.AppColors
import com.vibra.bus.presentation.viewmodel.NotificationsViewModel
import com.vibra.bus.util.toRelativeTime
import org.koin.compose.viewmodel.koinViewModel

class NotificationsScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel     = koinViewModel<NotificationsViewModel>()
        val notifications by viewModel.notifications.collectAsState()

        Scaffold(containerColor = AppColors.PrimaryBg) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {

                // Header morado
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.PrimaryPurple)
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                ) {
                    Text(
                        text       = "Notificaciones",
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = AppColors.White,
                    )
                }

                if (notifications.isEmpty()) {
                    EmptyState(
                        message  = "Sin notificaciones",
                        subtitle = "Aquí verás las alertas de tu bus",
                    )
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        items(notifications) { notif ->
                            NotificationCard(notif)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(notif: NotificationItem) {
    val (icon, color) = when (notif.type) {
        "bus_arrival"    -> Pair("🚌", AppColors.GreenActive)
        "bus_approaching"-> Pair("⏰", AppColors.AccentOrange)
        "bus_almost_full"-> Pair("⚠️", Color(0xFFF97316))
        else             -> Pair("🔔", AppColors.PrimaryPurple)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .shadow(2.dp, RoundedCornerShape(14.dp), ambientColor = AppColors.PrimaryPurple.copy(alpha = 0.07f))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(14.dp),
            colors   = CardDefaults.cardColors(containerColor = AppColors.White),
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
                    Text(notif.title, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary, fontSize = 14.sp)
                    Text(notif.body,  color = AppColors.TextSecondary, fontSize = 13.sp)
                }
                Text(notif.timestamp.toRelativeTime(), color = AppColors.TextSecondary, fontSize = 11.sp)
            }
        }
    }
}
