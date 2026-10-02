package com.vibra.bus.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.data.model.StopDto
import com.vibra.bus.data.repository.BrandRepository
import com.vibra.bus.presentation.components.BrandLogo
import com.vibra.bus.presentation.theme.LocalBrand
import kotlinx.coroutines.withTimeoutOrNull
import com.vibra.bus.presentation.viewmodel.AuthEvent
import com.vibra.bus.presentation.viewmodel.AuthViewModel
import com.vibra.bus.util.AppSettings
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

class SplashScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<AuthViewModel>()
        val settings  = koinInject<AppSettings>()
        val brandRepository = koinInject<BrandRepository>()
        val event     by viewModel.event.collectAsState()

        LaunchedEffect(Unit) {
            if (!brandRepository.hasOrganization) {
                navigator.replaceAll(OrganizationCodeScreen())
                return@LaunchedEffect
            }
            // Refresco de marca acotado: si la red tarda, se arranca con la marca cacheada.
            withTimeoutOrNull(2_500) { brandRepository.refresh() }
            viewModel.checkSession()
        }

        LaunchedEffect(event) {
            when (event) {
                is AuthEvent.NavigateToHome -> {
                    navigator.replaceAll(MainScreen())
                    if (settings.hasActiveTracking()) {
                        val stop = StopDto(
                            id           = settings.trackingStopId,
                            name         = settings.trackingStopName,
                            address      = settings.trackingStopAddress,
                            latitude     = settings.trackingStopLat,
                            longitude    = settings.trackingStopLng,
                            radiusMeters = 50,
                        )
                        startBusTracking(
                            settings.trackingPlate,
                            settings.trackingStopLat,
                            settings.trackingStopLng,
                            settings.trackingStopName,
                        )
                        navigator.push(WaitingBusScreen(settings.trackingPlate, stop))
                    }
                    viewModel.consumeEvent()
                }
                is AuthEvent.NavigateToLogin -> {
                    navigator.replaceAll(LoginScreen())
                    viewModel.consumeEvent()
                }
                else -> {}
            }
        }

        val brand = LocalBrand.current
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BrandLogo(Modifier.size(width = 200.dp, height = 132.dp))
                Spacer(Modifier.height(24.dp))
                Text(
                    text = brand.appName,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                brand.tagline?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(32.dp))
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}
