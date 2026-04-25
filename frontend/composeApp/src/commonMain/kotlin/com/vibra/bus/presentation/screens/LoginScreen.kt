package com.vibra.bus.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.presentation.theme.AppColors
import com.vibra.bus.presentation.theme.ButtonShape
import com.vibra.bus.presentation.theme.InputShape
import com.vibra.bus.presentation.viewmodel.AuthEvent
import com.vibra.bus.presentation.viewmodel.AuthViewModel
import com.vibra.bus.util.UiState
import org.koin.compose.viewmodel.koinViewModel

class LoginScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<AuthViewModel>()
        val uiState by viewModel.uiState.collectAsState()
        val event by viewModel.event.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        LaunchedEffect(event) {
            when (val e = event) {
                is AuthEvent.NavigateToHome -> {
                    navigator.replaceAll(MainScreen())
                    viewModel.consumeEvent()
                }
                is AuthEvent.ShowError -> {
                    snackbarHostState.showSnackbar(e.message)
                    viewModel.consumeEvent()
                }
                else -> {}
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = AppColors.PrimaryBg,
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(72.dp))
                Text(
                    text = "VIBRA +",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.AccentOrange,
                )
                Text(
                    text = "Bus UNAB",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.White,
                )
                Spacer(Modifier.height(48.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo institucional") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = InputShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.PrimaryPurple,
                        unfocusedBorderColor = AppColors.PrimaryPurple.copy(alpha = 0.5f),
                        focusedLabelColor = AppColors.PrimaryPurple,
                        unfocusedLabelColor = AppColors.White.copy(alpha = 0.7f),
                        focusedTextColor = AppColors.White,
                        unfocusedTextColor = AppColors.White,
                        cursorColor = AppColors.PrimaryPurple,
                    ),
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = InputShape,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.PrimaryPurple,
                        unfocusedBorderColor = AppColors.PrimaryPurple.copy(alpha = 0.5f),
                        focusedLabelColor = AppColors.PrimaryPurple,
                        unfocusedLabelColor = AppColors.White.copy(alpha = 0.7f),
                        focusedTextColor = AppColors.White,
                        unfocusedTextColor = AppColors.White,
                        cursorColor = AppColors.PrimaryPurple,
                    ),
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.login(email, password) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = ButtonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.AccentOrange),
                    enabled = uiState !is UiState.Loading,
                ) {
                    if (uiState is UiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = AppColors.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Iniciar Sesión", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = AppColors.White.copy(alpha = 0.3f),
                    )
                    Text(
                        text = "  O  ",
                        color = AppColors.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = AppColors.White.copy(alpha = 0.3f),
                    )
                }
                Spacer(Modifier.height(24.dp))
                Box {
                    Button(
                        onClick = { viewModel.loginWithGoogle() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = ButtonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.White),
                        enabled = uiState !is UiState.Loading,
                    ) {
                        if (uiState is UiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = AppColors.PrimaryBg,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = "Ingresar con Google",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.DarkText,
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).offset(y = (-8).dp).padding(end = 8.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = AppColors.GreenSuccess,
                    ) {
                        Text(
                            text = "Recomendado",
                            fontSize = 10.sp,
                            color = AppColors.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                TextButton(onClick = {}) {
                    Text("¿Olvidaste tu contraseña?", color = AppColors.AccentOrange, fontSize = 14.sp)
                }
                TextButton(onClick = {}) {
                    Text(
                        "¿No tienes cuenta? Contacta Soporte",
                        color = AppColors.GrayText,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}
