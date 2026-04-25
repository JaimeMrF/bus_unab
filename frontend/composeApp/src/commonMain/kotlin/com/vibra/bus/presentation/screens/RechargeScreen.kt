package com.vibra.bus.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.presentation.theme.AppColors

class RechargeScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Recargar saldo", color = AppColors.White) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, "Volver", tint = AppColors.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.PrimaryBg),
                )
            },
            containerColor = AppColors.PrimaryBg,
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text(
                    "Opciones de recarga",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.White,
                )
                Spacer(Modifier.height(16.dp))
                RechargeOption(
                    title = "Dentro de la universidad",
                    description = "Acércate al punto de recarga ubicado en el campus UNAB. Puedes recargar en efectivo o con tarjeta.",
                )
                Spacer(Modifier.height(12.dp))
                RechargeOption(
                    title = "Con efectivo",
                    description = "Recarga en cualquier punto autorizado de la red. Presenta tu código de estudiante para identificarte.",
                )
                Spacer(Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.MediumPurple.copy(alpha = 0.3f)),
                ) {
                    Text(
                        text = "ℹ️ Los pagos en línea estarán disponibles próximamente.",
                        color = AppColors.GrayText,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RechargeOption(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.DarkHeader),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = AppColors.White, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            Text(description, color = AppColors.GrayText, fontSize = 13.sp)
        }
    }
}
