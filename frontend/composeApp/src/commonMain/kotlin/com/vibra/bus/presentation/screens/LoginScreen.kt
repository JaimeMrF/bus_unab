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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.presentation.components.LoginInputField
import com.vibra.bus.presentation.components.PrimaryGlassButton
import com.vibra.bus.presentation.components.SecondaryGlassButton
import com.vibra.bus.presentation.theme.VibraBusShapes
import com.vibra.bus.presentation.theme.rubikGlitchFamily
import com.vibra.bus.presentation.viewmodel.AuthEvent
import com.vibra.bus.presentation.viewmodel.AuthViewModel
import com.vibra.bus.util.UiState
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import vibrabus.composeapp.generated.resources.Res
import vibrabus.composeapp.generated.resources.buhosaludologin
import vibrabus.composeapp.generated.resources.logo_unab_blanco_transparente

class LoginScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<AuthViewModel>()
        val uiState by viewModel.uiState.collectAsState()
        val event by viewModel.event.collectAsState()
        val snackbarState = remember { SnackbarHostState() }

        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var isFormVisible by remember { mutableStateOf(false) }

        val isLoading = uiState is UiState.Loading

        val logoScale by animateFloatAsState(
            targetValue = if (isFormVisible) 1f else 0.8f,
            animationSpec = tween(durationMillis = 800),
            label = "logo_scale"
        )

        val formAlpha by animateFloatAsState(
            targetValue = if (isFormVisible) 1f else 0f,
            animationSpec = tween(durationMillis = 1000, delayMillis = 300),
            label = "form_alpha"
        )

        LaunchedEffect(event) {
            when (val e = event) {
                is AuthEvent.NavigateToHome -> {
                    navigator.replaceAll(MainScreen())
                    viewModel.consumeEvent()
                }
                is AuthEvent.ShowError -> {
                    snackbarState.showSnackbar(e.message)
                    viewModel.consumeEvent()
                }
                else -> {}
            }
        }

        LaunchedEffect(Unit) {
            isFormVisible = true
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarState) },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer, // Morado oscuro
                                MaterialTheme.colorScheme.primary           // Morado UNAB
                            )
                        )
                    )
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {

                Spacer(Modifier.height(40.dp))

                // ── Mascota Búho ──────────────────────────────────────────────────
                AnimatedVisibility(
                    visible = isFormVisible,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(durationMillis = 800)
                    ) + fadeIn(animationSpec = tween(durationMillis = 800)),
                    exit = slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(durationMillis = 300)
                    ) + fadeOut(animationSpec = tween(durationMillis = 300))
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(logoScale),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.buhosaludologin),
                            contentDescription = "Búho Saludando",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Título ────────────────────────────────────────────────────────
                AnimatedVisibility(
                    visible = isFormVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 1000, delayMillis = 200)
                    ) + fadeIn(animationSpec = tween(durationMillis = 1000, delayMillis = 200)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 300))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.logo_unab_blanco_transparente),
                                contentDescription = "Logo UNAB",
                                modifier = Modifier.size(40.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Bus UNAB",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                letterSpacing = 1.sp
                            )
                        }
                        
                        Text(
                            text = "VIBRA+",
                            fontSize = 42.sp,
                            fontFamily = rubikGlitchFamily(),
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(48.dp))

                // ── Formulario ────────────────────────────────────────────────────
                AnimatedVisibility(
                    visible = isFormVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(durationMillis = 1200, delayMillis = 400)
                    ) + fadeIn(animationSpec = tween(durationMillis = 1200, delayMillis = 400)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 300))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 32.dp)
                            .alpha(formAlpha),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        LoginInputField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = "Correo electrónico",
                            icon = Icons.Default.Email,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        LoginInputField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = "Contraseña",
                            icon = Icons.Default.Lock,
                            isPassword = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )

                        // Olvidé contraseña
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            TextButton(
                                onClick = {},
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "¿Olvidaste tu contraseña?",
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        PrimaryGlassButton(
                            text = "Iniciar Sesión",
                            onClick = { viewModel.login(email, password) },
                            loading = isLoading,
                            enabled = email.isNotBlank() && password.isNotBlank()
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // ── Divisor ───────────────────────────────────────────────────────
                AnimatedVisibility(
                    visible = isFormVisible,
                    enter = fadeIn(animationSpec = tween(durationMillis = 800, delayMillis = 600))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .size(8.dp)
                                .clip(VibraBusShapes.RouteIndicator)
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f))
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Botón Google ──────────────────────────────────────────────────
                AnimatedVisibility(
                    visible = isFormVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 1000, delayMillis = 800)
                    ) + fadeIn(animationSpec = tween(durationMillis = 1000, delayMillis = 800))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SecondaryGlassButton(
                            text = "Ingresa con Google",
                            onClick = { viewModel.loginWithGoogle() },
                            showRecommendedBadge = true,
                            loading = isLoading,
                            enabled = !isLoading
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Enlace Registrarse ────────────────────────────────────────────
                AnimatedVisibility(
                    visible = isFormVisible,
                    enter = fadeIn(animationSpec = tween(durationMillis = 1200, delayMillis = 1000))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Text(
                            text = "¿No tienes cuenta?",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.width(4.dp))
                        TextButton(
                            onClick = { /* Navigate to register */ },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Regístrate",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}
