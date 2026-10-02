package com.vibra.bus.presentation.screens

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.vibra.bus.presentation.components.PillTone
import com.vibra.bus.presentation.components.StatusPill
import com.vibra.bus.presentation.components.ShimmerBox
import androidx.compose.material3.Surface
import com.vibra.bus.presentation.components.AppTopBar
import com.vibra.bus.presentation.components.BrandMascot
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import com.vibra.bus.presentation.theme.AppShape
import com.vibra.bus.presentation.viewmodel.MyQRViewModel
import com.vibra.bus.util.AppSettings
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import vibrabus.composeapp.generated.resources.Res

class MyQRScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<MyQRViewModel>()
        val settings  = koinInject<AppSettings>()
        val qrContent by viewModel.qrContent.collectAsState()
        val countdown by viewModel.countdown.collectAsState()

        var isVisible by remember { mutableStateOf(false) }

        val qrScale by animateFloatAsState(
            targetValue   = if (isVisible) 1f else 0.55f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessMediumLow,
            ),
            label = "qr_scale",
        )

        LaunchedEffect(Unit) { isVisible = true }

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            topBar = {
                AppTopBar(title = "Mi QR", onBack = if (navigator.canPop) ({ navigator.pop() }) else null)
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            AnimatedVisibility(
                visible = isVisible,
                enter   = fadeIn(tween(300)) + slideInVertically(
                    initialOffsetY = { it / 6 },
                    animationSpec  = tween(400),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                ) {
                    Text(
                        text      = "Presenta este código para abordar el bus",
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        style     = MaterialTheme.typography.bodyMedium,
                    )

                    Spacer(Modifier.height(24.dp))

                    val avatarUrl = settings.userAvatar
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(AppShape.Avatar)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = AppShape.Avatar,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (avatarUrl.isNotEmpty()) {
                            AsyncImage(
                                model              = avatarUrl,
                                contentDescription = "Avatar",
                                modifier           = Modifier.fillMaxSize(),
                                contentScale       = ContentScale.Crop,
                            )
                        } else {
                            BrandMascot(modifier = Modifier.fillMaxSize().padding(8.dp))
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text       = settings.userName,
                            color      = MaterialTheme.colorScheme.onSurface,
                            style      = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text  = "ID: ${settings.userId}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // QR card — spring bounce al entrar
                    Surface(
                        modifier        = Modifier.size(240.dp).scale(qrScale),
                        shape           = AppShape.QRContainer,
                        color           = Color.White,
                        shadowElevation = 8.dp,
                    ) {
                        Box(
                            modifier         = Modifier.fillMaxSize().padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (qrContent.isNotEmpty()) {
                                QRCodeImage(content = qrContent, modifier = Modifier.fillMaxSize())
                            } else {
                                ShimmerBox(height = 200.dp)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    StatusPill(
                        text = "Expira en ${countdown}s",
                        tone = if (countdown > 10) PillTone.Brand else PillTone.Error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text      = "Este código es personal e intransferible",
                        style     = MaterialTheme.typography.bodySmall,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
