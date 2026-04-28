package com.vibra.bus.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

class RechargeScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = {
                TopAppBar(
                    title          = { Text("Recargar saldo", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                Text(
                    "Elige tu método de recarga",
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Recarga tu saldo de forma fácil y rápida",
                    fontSize = 13.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
                )

                RechargeOption(
                    emoji       = "🏫",
                    title       = "Dentro de la universidad",
                    description = "Acércate al punto de recarga ubicado en el campus UNAB. Puedes recargar en efectivo o con tarjeta.",
                )
                Spacer(Modifier.height(12.dp))
                RechargeOption(
                    emoji       = "💵",
                    title       = "Con efectivo",
                    description = "Recarga en cualquier punto autorizado de la red. Presenta tu código de estudiante para identificarte.",
                )
                Spacer(Modifier.height(24.dp))

                // Info card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f))
                        .padding(16.dp),
                ) {
                    Text(
                        text     = "ℹ️ Los pagos en línea estarán disponibles próximamente.",
                        color    = MaterialTheme.colorScheme.secondary,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun RechargeOption(emoji: String, title: String, description: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.onPrimary)
            .padding(16.dp),
    ) {
        Column {
            Text(
                "$emoji  $title",
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
                fontSize   = 15.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
    }
}
