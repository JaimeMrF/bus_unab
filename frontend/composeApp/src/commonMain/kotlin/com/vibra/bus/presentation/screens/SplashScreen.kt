package com.vibra.bus.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.presentation.theme.AppColors
import com.vibra.bus.presentation.viewmodel.AuthEvent
import com.vibra.bus.presentation.viewmodel.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel

class SplashScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<AuthViewModel>()
        val event by viewModel.event.collectAsState()

        LaunchedEffect(Unit) {
            viewModel.checkSession()
        }

        LaunchedEffect(event) {
            when (event) {
                is AuthEvent.NavigateToHome -> {
                    navigator.replaceAll(MainScreen())
                    viewModel.consumeEvent()
                }
                is AuthEvent.NavigateToLogin -> {
                    navigator.replaceAll(LoginScreen())
                    viewModel.consumeEvent()
                }
                else -> {}
            }
        }

        Box(
            modifier = Modifier.fillMaxSize().background(AppColors.PrimaryBg),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "VIBRA +",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.AccentOrange,
                )
                Text(
                    text = "Bus UNAB",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.White,
                )
                CircularProgressIndicator(color = AppColors.AccentOrange)
            }
        }
    }
}
