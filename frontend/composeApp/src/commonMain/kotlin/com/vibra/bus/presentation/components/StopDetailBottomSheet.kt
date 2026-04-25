package com.vibra.bus.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibra.bus.data.model.StopDto
import com.vibra.bus.presentation.theme.AppColors
import com.vibra.bus.presentation.theme.BottomSheetShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopDetailBottomSheet(
    stop: StopDto,
    onDismiss: () -> Unit,
    onOpenMaps: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = BottomSheetShape,
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stop.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.DarkText,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stop.address,
                fontSize = 14.sp,
                color = AppColors.GrayText,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Radio de detección: ${stop.radiusMeters} metros",
                fontSize = 14.sp,
                color = AppColors.MediumPurple,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onOpenMaps,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.AccentOrange),
            ) {
                Text("Abrir en Google Maps", color = AppColors.White)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
