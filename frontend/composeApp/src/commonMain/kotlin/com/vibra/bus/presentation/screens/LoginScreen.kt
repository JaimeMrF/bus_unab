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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.vibra.bus.presentation.theme.rubikGlitchFamily
import com.vibra.bus.presentation.viewmodel.AuthEvent
import com.vibra.bus.presentation.viewmodel.AuthViewModel
import com.vibra.bus.util.UiState
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import vibrabus.composeapp.generated.resources.Res
import vibrabus.composeapp.generated.resources.leopardo_saludo

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
        var showDriverForm by remember { mutableStateOf(false) }

        val isLoading = uiState is UiState.Loading

        val logoScale by animateFloatAsState(
            targetValue = if (isFormVisible) 1f else 0.8f,
            animationSpec = tween(durationMillis = 800),
            label = "logo_scale"
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
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primary
                            )
                        )
                    )
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (showDriverForm) Arrangement.Top else Arrangement.Center
            ) {

                if (showDriverForm) {
                    Spacer(Modifier.height(24.dp))
                }

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
                    Image(
                        painter = painterResource(Res.drawable.leopardo_saludo),
                        contentDescription = "Búho Saludando",
                        modifier = Modifier
                            .size(220.dp) // ← ligeramente más pequeño para que quepa todo
                            .scale(logoScale),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(Modifier.height(4.dp)) // ← era 8dp

                // ── Logo + BUCARATRANSIT ───────────────────────────────────
                AnimatedVisibility(
                    visible = isFormVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 1000, delayMillis = 200)
                    ) + fadeIn(animationSpec = tween(durationMillis = 1000, delayMillis = 200)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 300))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "BUCARATRANSIT",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Plataforma de Movilidad",
                                fontSize = 48.sp,
                                fontFamily = rubikGlitchFamily(),
                                color = MaterialTheme.colorScheme.secondary,
                                letterSpacing = 2.sp,
                                lineHeight = 50.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Botón Google (principal) ──────────────────────────────────────
                AnimatedVisibility(
                    visible = isFormVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 1000, delayMillis = 400)
                    ) + fadeIn(animationSpec = tween(durationMillis = 1000, delayMillis = 400))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFE8A33D).copy(alpha = 0.18f),
                                        Color(0xFFE8A33D).copy(alpha = 0.08f)
                                    )
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFE8A33D).copy(alpha = 0.8f),
                                        Color(0xFFE8A33D).copy(alpha = 0.4f)
                                    )
                                ),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "⭐  TODA BUCARAMANGA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFE8A33D),
                                letterSpacing = 2.sp
                            )
                            SecondaryGlassButton(
                                text = "Ingresa con Google",
                                onClick = { viewModel.loginWithGoogle() },
                                showRecommendedBadge = false,
                                loading = isLoading,
                                enabled = !isLoading
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Formulario conductor (oculto por defecto) ─────────────────────
                AnimatedVisibility(
                    visible = showDriverForm,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 400)
                    ) + fadeIn(animationSpec = tween(durationMillis = 400)),
                    exit = slideOutVertically(
                        targetOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 300)
                    ) + fadeOut(animationSpec = tween(durationMillis = 300))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                            Text(
                                text = "  Acceso conductores  ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        }

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

                        PrimaryGlassButton(
                            text = "Iniciar Sesión",
                            onClick = { viewModel.login(email, password) },
                            loading = isLoading,
                            enabled = email.isNotBlank() && password.isNotBlank()
                        )
                    }
                }

                // ── Botón ¿Eres conductor? ────────────────────────────────────────
                AnimatedVisibility(
                    visible = isFormVisible,
                    enter = fadeIn(animationSpec = tween(durationMillis = 800, delayMillis = 600))
                ) {
                    TextButton(
                        onClick = { showDriverForm = !showDriverForm },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (showDriverForm) "← No soy conductor" else "¿Eres conductor?",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (showDriverForm) {
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}