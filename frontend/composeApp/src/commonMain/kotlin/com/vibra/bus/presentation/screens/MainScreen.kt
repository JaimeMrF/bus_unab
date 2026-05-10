package com.vibra.bus.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.transitions.SlideTransition
import com.vibra.bus.presentation.components.BottomNavBar
import com.vibra.bus.presentation.components.NavItem
import com.vibra.bus.presentation.components.driverNavItems
import com.vibra.bus.presentation.components.studentNavItems
import com.vibra.bus.presentation.viewmodel.ProfileViewModel
import org.koin.compose.viewmodel.koinViewModel

class MainScreen : Screen {

    @Composable
    override fun Content() {
        val profileViewModel = koinViewModel<ProfileViewModel>()
        val profile by profileViewModel.profile.collectAsState()
        val isDriver = profile.role == "driver"
        val navItems = if (isDriver) driverNavItems() else studentNavItems()
        var selectedRoute by remember { mutableStateOf("home") }

        Scaffold(
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
                    "home"          -> Navigator(HomeScreen()) { SlideTransition(it) }
                    "trips"         -> if (isDriver) Navigator(DriverModeScreen()) { SlideTransition(it) }
                                       else Navigator(MyTripsScreen()) { SlideTransition(it) }
                    "qr"            -> Navigator(MyQRScreen()) { SlideTransition(it) }
                    "scanner"       -> Navigator(QRScannerScreen()) { SlideTransition(it) }
                    "notifications" -> Navigator(NotificationsScreen()) { SlideTransition(it) }
                    "profile"       -> Navigator(ProfileScreen()) { SlideTransition(it) }
                }
            }
        }
    }
}
