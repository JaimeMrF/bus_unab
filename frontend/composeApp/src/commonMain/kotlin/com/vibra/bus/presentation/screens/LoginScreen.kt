package com.vibra.bus.presentation.screens

import com.vibra.bus.domain.brand.MascotPose
import com.vibra.bus.presentation.components.BrandMascot
import com.vibra.bus.presentation.motion.auroraBackground
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.data.repository.BrandRepository
import com.vibra.bus.presentation.components.AppTextField
import com.vibra.bus.presentation.components.BrandLogo
import com.vibra.bus.presentation.components.GoogleSignInButton
import com.vibra.bus.presentation.components.PrimaryButton
import com.vibra.bus.presentation.components.Staggered
import com.vibra.bus.presentation.theme.LocalBrand
import com.vibra.bus.presentation.theme.Sizing
import com.vibra.bus.presentation.viewmodel.AuthEvent
import com.vibra.bus.presentation.viewmodel.AuthViewModel
import com.vibra.bus.util.UiState
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

class LoginScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<AuthViewModel>()
        val brandRepository = koinInject<BrandRepository>()
        val brand = LocalBrand.current
        val uiState by viewModel.uiState.collectAsState()
        val event by viewModel.event.collectAsState()
        val snackbarState = remember { SnackbarHostState() }

        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var showDriverForm by remember { mutableStateOf(false) }

        val isLoading = uiState is UiState.Loading
        val canSubmit = email.isNotBlank() && password.isNotBlank() && !isLoading

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

        val glow = MaterialTheme.colorScheme.primary
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarState) },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Aurora con los colores del tenant (estática con "reducir movimiento").
                    .auroraBackground()
                    .padding(padding)
                    .imePadding(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 28.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Staggered(0) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            BrandMascot(pose = MascotPose.Greeting, size = 120.dp)
                            BrandLogo(Modifier.size(width = 168.dp, height = 112.dp))
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Staggered(1) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = brand.appName,
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                            )
                            brand.tagline?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(40.dp))

                    Staggered(2) {
                        GoogleSignInButton(
                            text = "Continuar con Google",
                            onClick = { viewModel.loginWithGoogle() },
                            loading = isLoading && !showDriverForm,
                            enabled = !isLoading,
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Staggered(3) {
                        TextButton(
                            onClick = { showDriverForm = !showDriverForm },
                            modifier = Modifier.heightIn(min = Sizing.touchTarget),
                        ) {
                            Text(
                                text = if (showDriverForm) "Ocultar acceso de conductores" else "Acceso de conductores",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showDriverForm,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            AppTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = "Correo electrónico",
                                icon = Icons.Default.Email,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next,
                                ),
                            )
                            AppTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = "Contraseña",
                                icon = Icons.Default.Lock,
                                isPassword = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (canSubmit) viewModel.login(email, password)
                                }),
                            )
                            PrimaryButton(
                                text = "Iniciar sesión",
                                onClick = { viewModel.login(email, password) },
                                loading = isLoading && showDriverForm,
                                enabled = canSubmit,
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    TextButton(
                        onClick = {
                            brandRepository.clearOrganization()
                            navigator.replaceAll(OrganizationCodeScreen())
                        },
                    ) {
                        Text(
                            text = "Cambiar organización",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

