package com.vibra.bus.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.vibra.bus.presentation.theme.AppColors
import com.vibra.bus.presentation.viewmodel.MyQRViewModel
import com.vibra.bus.util.AppSettings
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

class MyQRScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator  = LocalNavigator.currentOrThrow
        val viewModel  = koinViewModel<MyQRViewModel>()
        val settings   = koinInject<AppSettings>()
        val qrContent  by viewModel.qrContent.collectAsState()
        val countdown  by viewModel.countdown.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title            = { Text("Mi QR", color = AppColors.White, fontWeight = FontWeight.SemiBold) },
                    navigationIcon   = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, "Volver", tint = AppColors.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.PrimaryPurple),
                )
            },
            containerColor = AppColors.PrimaryBg,
        ) { padding ->
            Column(
                modifier            = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
            ) {
                Text(
                    text      = "Presenta este código para abordar el bus",
                    fontSize  = 14.sp,
                    color     = AppColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))

                // Avatar
                val avatarUrl = settings.userAvatar
                if (avatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model              = avatarUrl,
                        contentDescription = "Avatar",
                        modifier           = Modifier.size(80.dp).clip(CircleShape),
                    )
                } else {
                    Box(
                        modifier         = Modifier
                            .size(80.dp)
                            .background(AppColors.PrimaryPurple, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text       = settings.userName.firstOrNull()?.uppercase() ?: "U",
                            fontSize   = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color      = AppColors.White,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(settings.userName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.TextPrimary)
                Text("ID: ${settings.userId}", fontSize = 13.sp, color = AppColors.TextSecondary)
                Spacer(Modifier.height(24.dp))

                // QR Card
                Card(
                    modifier  = Modifier.size(240.dp),
                    shape     = RoundedCornerShape(20.dp),
                    colors    = CardDefaults.cardColors(containerColor = AppColors.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                ) {
                    Box(
                        modifier         = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (qrContent.isNotEmpty()) {
                            QRCodeImage(content = qrContent)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                // Countdown badge
                Box(
                    modifier = Modifier
                        .background(
                            color = if (countdown > 10) AppColors.GreenSuccess.copy(alpha = 0.12f)
                                    else AppColors.Red.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text       = "Expira en ${countdown}s",
                        color      = if (countdown > 10) AppColors.GreenSuccess else AppColors.Red,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    text      = "ⓘ Este código es personal e intransferible",
                    fontSize  = 12.sp,
                    color     = AppColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
expect fun QRCodeImage(content: String, modifier: Modifier = Modifier)
