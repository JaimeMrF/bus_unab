package com.vibra.bus.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.vibra.bus.presentation.components.BottomNavBar
import com.vibra.bus.presentation.components.driverNavItems
import com.vibra.bus.presentation.components.studentNavItems
import com.vibra.bus.presentation.theme.VibraBusTheme
import com.vibra.bus.presentation.viewmodel.ProfileViewModel
import com.vibra.bus.util.AppSettings
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

class MainScreen : Screen {

    @Composable
    override fun Content() {
        val settings         = koinInject<AppSettings>()
        val profileViewModel = koinViewModel<ProfileViewModel>()
        val profile   by profileViewModel.profile.collectAsState()
        val isDark by settings.isDarkThemeFlow.collectAsState()
        val isDriver  = profile.role == "driver"
        val navItems  = if (isDriver) driverNavItems() else studentNavItems()
        var selectedRoute by remember { mutableStateOf("home") }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                BottomNavBar(
                    items = navItems,
                    selectedRoute = selectedRoute,
                    onTabSelected = { item -> selectedRoute = item.route },
                )
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (selectedRoute) {
                    "home"          -> Navigator(HomeScreen())          { VibraBusTheme(isDark) { SlideTransition(it) } }
                    "trips"         -> if (isDriver)
                                           Navigator(DriverModeScreen()) { VibraBusTheme(isDark) { SlideTransition(it) } }
                                       else
                                           Navigator(MyTripsScreen())    { VibraBusTheme(isDark) { SlideTransition(it) } }
                    "qr"            -> Navigator(MyQRScreen())           { VibraBusTheme(isDark) { SlideTransition(it) } }
                    "scanner"       -> Navigator(QRScannerScreen())      { VibraBusTheme(isDark) { SlideTransition(it) } }
                    "notifications" -> Navigator(NotificationsScreen())  { VibraBusTheme(isDark) { SlideTransition(it) } }
                    "profile"       -> Navigator(ProfileScreen())        { VibraBusTheme(isDark) { SlideTransition(it) } }
                }
            }
        }
    }
}
