package com.vibra.bus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.vibra.bus.presentation.screens.SplashScreen
import com.vibra.bus.presentation.theme.LocalIsDarkTheme
import com.vibra.bus.presentation.theme.LocalThemeToggle
import com.vibra.bus.presentation.theme.VibraBusTheme
import com.vibra.bus.util.AppSettings
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

@Composable
fun App() {
    KoinContext {
        val settings = koinInject<AppSettings>()
        val isDark by settings.isDarkThemeFlow.collectAsState()

        CompositionLocalProvider(
            LocalIsDarkTheme provides isDark,
            LocalThemeToggle provides { newValue ->
                settings.isDarkTheme = newValue
            },
        ) {
            Navigator(SplashScreen()) { navigator ->
                VibraBusTheme(darkTheme = isDark) {
                    SlideTransition(navigator)
                }
            }
        }
    }
}
