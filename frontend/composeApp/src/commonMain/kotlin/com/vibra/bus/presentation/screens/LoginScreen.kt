package com.vibra.bus.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.presentation.theme.AppColors
import com.vibra.bus.presentation.viewmodel.AuthEvent
import com.vibra.bus.presentation.viewmodel.AuthViewModel
import com.vibra.bus.util.UiState
import org.koin.compose.viewmodel.koinViewModel

class LoginScreen : Screen {

    @Composable
    override fun Content() {
        val navigator     = LocalNavigator.currentOrThrow
        val viewModel     = koinViewModel<AuthViewModel>()
        val uiState       by viewModel.uiState.collectAsState()
        val event         by viewModel.event.collectAsState()
        val snackbarState = remember { SnackbarHostState() }

        var email    by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        val isLoading = uiState is UiState.Loading

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
            snackbarHost   = { SnackbarHost(snackbarState) },
            containerColor = AppColors.White,
        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            ) {

                // ── Banda superior con acento dorado ─────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(AppColors.AccentOrange)
                )

                Spacer(Modifier.height(52.dp))

                // ── Sección de marca (izquierda) ──────────────────────────
                Column(
                    modifier = Modifier.padding(horizontal = 32.dp),
                ) {

                    // Etiqueta pequeña morada
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(2.dp)
                                .background(AppColors.AccentOrange)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text       = "BUS UNAB",
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color      = AppColors.AccentOrange,
                            letterSpacing = 3.sp,
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Logo principal
                    Text(
                        text          = "VIBRA+",
                        fontSize      = 52.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        color         = AppColors.PrimaryPurple,
                        letterSpacing = (-1).sp,
                        lineHeight    = 52.sp,
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text       = "Inicia sesión para\ncontinuar",
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Light,
                        color      = AppColors.TextPrimary,
                        lineHeight = 30.sp,
                    )
                }

                Spacer(Modifier.height(48.dp))

                // ── Formulario con campos subrayados ──────────────────────
                Column(
                    modifier = Modifier.padding(horizontal = 32.dp),
                ) {

                    // Campo correo — underline style
                    TextField(
                        value           = email,
                        onValueChange   = { email = it },
                        label           = {
                            Text(
                                "Correo institucional",
                                fontSize = 13.sp,
                            )
                        },
                        modifier        = Modifier.fillMaxWidth(),
                        singleLine      = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors          = underlineFieldColors(),
                    )

                    Spacer(Modifier.height(20.dp))

                    // Campo contraseña — underline style
                    TextField(
                        value                = password,
                        onValueChange        = { password = it },
                        label                = {
                            Text(
                                "Contraseña",
                                fontSize = 13.sp,
                            )
                        },
                        modifier             = Modifier.fillMaxWidth(),
                        singleLine           = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors               = underlineFieldColors(),
                    )

                    // Enlace olvidé contraseña
                    Box(
                        modifier         = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        TextButton(
                            onClick        = {},
                            contentPadding = PaddingValues(vertical = 6.dp),
                        ) {
                            Text(
                                text       = "¿Olvidaste tu contraseña?",
                                color      = AppColors.AccentOrange,
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    // ── Botón principal morado ────────────────────────────
                    Button(
                        onClick  = { viewModel.login(email, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape  = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor         = AppColors.PrimaryPurple,
                            disabledContainerColor = AppColors.PrimaryPurple.copy(alpha = 0.40f),
                        ),
                        enabled = !isLoading,
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(22.dp),
                                color       = AppColors.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text          = "Iniciar Sesión",
                                fontSize      = 16.sp,
                                fontWeight    = FontWeight.SemiBold,
                                color         = AppColors.White,
                                letterSpacing = 0.5.sp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(36.dp))

                // ── Divisor ───────────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color    = AppColors.TextSecondary.copy(alpha = 0.20f),
                    )
                    Text(
                        text     = "   o   ",
                        color    = AppColors.TextSecondary,
                        fontSize = 13.sp,
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color    = AppColors.TextSecondary.copy(alpha = 0.20f),
                    )
                }

                Spacer(Modifier.height(24.dp))

                // ── Botón Google ──────────────────────────────────────────
                Column(
                    modifier = Modifier.padding(horizontal = 32.dp),
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick  = { viewModel.loginWithGoogle() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape  = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor         = AppColors.White,
                                contentColor           = AppColors.TextPrimary,
                                disabledContainerColor = AppColors.White,
                            ),
                            border  = androidx.compose.foundation.BorderStroke(
                                width = 1.5.dp,
                                color = AppColors.PrimaryPurple.copy(alpha = 0.30f),
                            ),
                            enabled = !isLoading,
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(22.dp),
                                    color       = AppColors.PrimaryPurple,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text(
                                    text       = "Ingresar con Google",
                                    fontSize   = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color      = AppColors.TextPrimary,
                                )
                            }
                        }

                        // Badge "Recomendado"
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(AppColors.AccentOrange.copy(alpha = 0.10f))
                                .border(
                                    width = 1.dp,
                                    color = AppColors.AccentOrange,
                                    shape = RoundedCornerShape(4.dp),
                                )
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text       = "Recomendado",
                                fontSize   = 10.sp,
                                color      = AppColors.AccentOrange,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    TextButton(
                        onClick  = {},
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(
                            text      = "¿No tienes cuenta? Contacta Soporte",
                            color     = AppColors.TextSecondary,
                            fontSize  = 13.sp,
                            textAlign = TextAlign.Center,
                        )
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

/** Campos estilo subrayado — sin caja, solo línea inferior morada. */
@Composable
private fun underlineFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor      = Color.Transparent,
    unfocusedContainerColor    = Color.Transparent,
    disabledContainerColor     = Color.Transparent,
    focusedIndicatorColor      = AppColors.PrimaryPurple,
    unfocusedIndicatorColor    = AppColors.TextSecondary.copy(alpha = 0.30f),
    focusedLabelColor          = AppColors.PrimaryPurple,
    unfocusedLabelColor        = AppColors.TextSecondary,
    focusedTextColor           = AppColors.TextPrimary,
    unfocusedTextColor         = AppColors.TextPrimary,
    cursorColor                = AppColors.PrimaryPurple,
)
