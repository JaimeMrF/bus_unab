package com.vibra.bus

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.FadeTransition
import com.vibra.bus.data.repository.BrandRepository
import com.vibra.bus.presentation.screens.SplashScreen
import com.vibra.bus.presentation.theme.AppTheme
import com.vibra.bus.presentation.theme.LocalThemeToggle
import com.vibra.bus.util.AppSettings
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

@Composable
fun App() {
    KoinContext {
        val settings = koinInject<AppSettings>()
        val brandRepository = koinInject<BrandRepository>()
        val themeMode by settings.themeModeFlow.collectAsState()
        val isDark = when (themeMode) {
            "dark" -> true
            "light" -> false
            else -> isSystemInDarkTheme()
        }
        val brand by brandRepository.brand.collectAsState()

        // Un solo AppTheme en la raíz: cualquier cambio de BrandConfig reestiliza toda la app.
        AppTheme(brand = brand, darkTheme = isDark) {
            CompositionLocalProvider(
                LocalThemeToggle provides { newValue -> settings.themeMode = if (newValue) "dark" else "light" },
            ) {
                Navigator(SplashScreen()) { navigator ->
                    FadeTransition(navigator)
                }
            }
        }
    }
}
