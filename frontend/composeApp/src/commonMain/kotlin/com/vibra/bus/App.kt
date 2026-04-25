package com.vibra.bus

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.vibra.bus.presentation.screens.SplashScreen
import com.vibra.bus.presentation.theme.VibraBusTheme
import org.koin.compose.KoinContext

@Composable
fun App() {
    KoinContext {
        VibraBusTheme {
            Navigator(SplashScreen()) { navigator ->
                SlideTransition(navigator)
            }
        }
    }
}
