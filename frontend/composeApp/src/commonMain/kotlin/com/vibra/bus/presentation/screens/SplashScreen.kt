package com.vibra.bus.presentation.screens

import com.vibra.bus.presentation.theme.Motion
import com.vibra.bus.presentation.motion.auroraBackground
import com.vibra.bus.presentation.motion.MotionSpec
import com.vibra.bus.presentation.motion.LocalMotion
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.remember
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
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
        val env = LocalMotion.current

        // Entrada del logo (resorte) y salida (escala + fade) antes de navegar. Se leen en graphicsLayer.
        val entry = remember { Animatable(if (env.animate) 0f else 1f) }
        val exit = remember { Animatable(0f) }
        suspend fun playExit() {
            if (env.animate) exit.animateTo(1f, tween(MotionSpec.Standard, easing = Motion.easeInOut))
        }
        LaunchedEffect(Unit) {
            if (env.animate) entry.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
        }

        LaunchedEffect(Unit) {
            if (!brandRepository.hasOrganization) {
                playExit()
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
                    playExit()
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
                    playExit()
                    navigator.replaceAll(LoginScreen())
                    viewModel.consumeEvent()
                }
                else -> {}
            }
        }

        val brand = LocalBrand.current
        Box(
            modifier = Modifier.fillMaxSize().auroraBackground(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    val e = entry.value
                    val x = exit.value
                    val s = (0.8f + 0.2f * e) * (1f + 0.14f * x)
                    scaleX = s
                    scaleY = s
                    alpha = e * (1f - x)
                },
            ) {
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
