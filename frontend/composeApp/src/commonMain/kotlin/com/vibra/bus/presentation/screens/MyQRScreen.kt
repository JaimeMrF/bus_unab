package com.vibra.bus.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.vibra.bus.presentation.screens.QRCodeImage
import com.vibra.bus.presentation.theme.VibraBusShapes
import com.vibra.bus.presentation.viewmodel.MyQRViewModel
import com.vibra.bus.util.AppSettings
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import vibrabus.composeapp.generated.resources.Res
import vibrabus.composeapp.generated.resources.buhosaludologin

class MyQRScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<MyQRViewModel>()
        val settings = koinInject<AppSettings>()
        val qrContent by viewModel.qrContent.collectAsState()
        val countdown by viewModel.countdown.collectAsState()

        var isVisible by remember { mutableStateOf(false) }

        // Animation states
        val qrScale by animateFloatAsState(
            targetValue = if (isVisible) 1f else 0.8f,
            animationSpec = tween(durationMillis = 800),
            label = "qr_scale"
        )

        val containerScale by animateFloatAsState(
            targetValue = if (isVisible) 1f else 0.9f,
            animationSpec = tween(durationMillis = 600),
            label = "container_scale"
        )

        LaunchedEffect(Unit) {
            isVisible = true
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Mi QR",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                Icons.AutoMirrored.Default.ArrowBack,
                                "Volver",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
            ) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(durationMillis = 600)
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 600)
                    )
                ) {
                    Text(
                        text = "Presenta este código para abordar el bus",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Avatar con la identidad del Búho UNAB
                val avatarUrl = settings.userAvatar
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 800, delayMillis = 200)
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 800, delayMillis = 200)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(VibraBusShapes.OwlAvatar)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = VibraBusShapes.OwlAvatar
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarUrl.isNotEmpty()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(Res.drawable.buhosaludologin),
                                contentDescription = "Búho UNAB",
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(
                        animationSpec = tween(durationMillis = 1000, delayMillis = 400)
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = settings.userName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "ID: ${settings.userId}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // QR Card con contenedor blanco puro y diseño limpio
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 1200, delayMillis = 600)
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 1200, delayMillis = 600)
                    )
                ) {
                    Card(
                        modifier = Modifier
                            .size(240.dp)
                            .scale(qrScale),
                        shape = VibraBusShapes.QRContainer,
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White // Contenedor blanco puro
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 8.dp
                        ),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (qrContent.isNotEmpty()) {
                                QRCodeImage(content = qrContent, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Countdown badge optimizado
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 1400, delayMillis = 800)
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 1400, delayMillis = 800)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .scale(containerScale)
                            .background(
                                color = if (countdown > 10)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                shape = VibraBusShapes.StatusBadge,
                            )
                            .border(
                                width = 1.dp,
                                color = if (countdown > 10)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error,
                                shape = VibraBusShapes.StatusBadge
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = "Expira en ${countdown}s",
                            color = if (countdown > 10)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(
                        animationSpec = tween(durationMillis = 1600, delayMillis = 1000)
                    )
                ) {
                    Text(
                        text = "ⓘ Este código es personal e intransferible",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
