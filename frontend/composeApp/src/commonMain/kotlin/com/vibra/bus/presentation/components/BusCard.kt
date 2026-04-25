package com.vibra.bus.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibra.bus.data.model.BusSummaryDto
import com.vibra.bus.data.model.OccupancyDto
import com.vibra.bus.presentation.theme.AppColors
import com.vibra.bus.presentation.theme.CardShape

@Composable
fun BusCard(
    bus: BusSummaryDto,
    occupancy: OccupancyDto?,
    onClick: () -> Unit,
) {
    val isFull = occupancy?.level == "full"
    val isAvailable = occupancy != null
    val borderColor = when {
        isFull -> AppColors.Red
        isAvailable -> AppColors.CardActive
        else -> AppColors.CardBorder
    }
    val occupancyColor = when (occupancy?.level) {
        "low" -> AppColors.GreenActive
        "medium" -> AppColors.AccentOrange
        "high" -> Color(0xFFF97316)
        "full" -> Color(0xFFEF4444)
        else -> AppColors.GrayText
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = CardShape,
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isAvailable) {
                        Surface(
                            shape = CircleShape,
                            color = AppColors.GreenActive,
                            modifier = Modifier.size(8.dp),
                        ) {}
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = bus.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = AppColors.DarkText,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Placa: ${bus.plate}",
                    fontSize = 12.sp,
                    color = AppColors.GrayText,
                )
                if (isFull) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Bus lleno",
                        fontSize = 12.sp,
                        color = AppColors.Red,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            occupancy?.let {
                Column(horizontalAlignment = Alignment.End) {
                    OccupancyBadge(level = it.level, percentage = it.percentage, color = occupancyColor)
                }
            }
        }
    }
}

@Composable
fun OccupancyBadge(level: String, percentage: Float, color: Color) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "${percentage.toInt()}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                text = level.replaceFirstChar { it.uppercase() },
                fontSize = 10.sp,
                color = color,
            )
        }
    }
}
