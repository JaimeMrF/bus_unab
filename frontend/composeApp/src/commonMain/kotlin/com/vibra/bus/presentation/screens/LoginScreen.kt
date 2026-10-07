package com.vibra.bus.presentation.screens

import com.vibra.bus.util.isGoogleSignInAvailable
import com.vibra.bus.domain.brand.MascotPose
import com.vibra.bus.presentation.components.BrandMascot
import com.vibra.bus.presentation.components.NeutralBadge
import com.vibra.bus.presentation.motion.auroraBackground
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.data.repository.BrandRepository
import com.vibra.bus.presentation.components.AppTextField
import com.vibra.bus.presentation.components.BRAND_LOGO_ASPECT
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

/** Columna de contenido: misma medida en todas las pantallas de entrada. */
private val CONTENT_MAX_WIDTH = 480.dp

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
        // Sin Google (iOS sin SDK o client id ausente) el formulario de correo es el acceso principal.
        var showDriverForm by remember { mutableStateOf(!isGoogleSignInAvailable) }

        val isLoading = uiState is UiState.Loading
        val canSubmit = email.isNotBlank() && password.isNotBlank() && !isLoading

        // Sin organización cargada no hay logo ni mascota que mostrar: la marca del tema neutro
        // ("Transporte") no es la del usuario, así que el encabezado cae a un distintivo propio.
        val branded = brand.hasBrand
        val headline = when {
            brand.tagline?.isNotBlank() == true -> brand.tagline!!
            branded -> brand.appName
            else -> "Bienvenido"
        }

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
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    // La marca es la protagonista: el logo abre la pantalla y el leopardo ocupa el
                    // centro a tamaño de héroe, no un hueco de 120dp sobre un formulario.
                    val logoWidth = (maxWidth * 0.46f).coerceIn(140.dp, 208.dp)
                    val mascotSize = (maxWidth * 0.60f).coerceIn(176.dp, 272.dp)

                    Column(
                        modifier = Modifier
                            .widthIn(max = CONTENT_MAX_WIDTH)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 28.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Staggered(0) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (branded) {
                                    BrandLogo(Modifier.width(logoWidth).aspectRatio(BRAND_LOGO_ASPECT))
                                    Spacer(Modifier.height(12.dp))
                                    BrandMascot(pose = MascotPose.Greeting, size = mascotSize)
                                } else {
                                    NeutralBadge(Modifier.size(112.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Staggered(1) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = headline,
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Center,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (!branded) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Ingresa con tu cuenta para continuar.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(28.dp))

                        if (isGoogleSignInAvailable) Staggered(2) {
                            GoogleSignInButton(
                                text = "Continuar con Google",
                                onClick = { viewModel.loginWithGoogle() },
                                loading = isLoading && !showDriverForm,
                                enabled = !isLoading,
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        if (isGoogleSignInAvailable) Staggered(3) {
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

                        Spacer(Modifier.height(20.dp))

                        TextButton(
                            onClick = {
                                brandRepository.clearOrganization()
                                navigator.replaceAll(OrganizationCodeScreen())
                            },
                            modifier = Modifier.heightIn(min = Sizing.touchTarget),
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
}
