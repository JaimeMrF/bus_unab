package com.vibra.bus.presentation.screens

import com.vibra.bus.domain.brand.MascotPose
import com.vibra.bus.presentation.components.BRAND_LOGO_ASPECT
import com.vibra.bus.presentation.components.BrandMascot
import kotlinx.coroutines.launch
import com.vibra.bus.presentation.theme.Motion
import com.vibra.bus.presentation.motion.auroraBackground
import com.vibra.bus.presentation.motion.MotionSpec
import com.vibra.bus.presentation.motion.LocalMotion
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
            // Con marca cacheada no espera (refresca en segundo plano); en una instalación nueva
            // espera hasta 4 s a la primera respuesta, y si no llega el tema se actualiza solo después.
            brandRepository.awaitBrand()
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
                // El tema neutro no es la marca del usuario: sin organización cargada solo se muestra
                // el progreso (antes aparecía "Transporte" al abrir la app por primera vez). Cuando la
                // marca llega, el lockup y el leopardo entran con resorte.
                AnimatedVisibility(
                    visible = brand.hasBrand,
                    enter = if (env.animate) {
                        fadeIn(tween(MotionSpec.Standard)) +
                            scaleIn(spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow), initialScale = 0.86f)
                    } else EnterTransition.None,
                    exit = if (env.animate) fadeOut(tween(Motion.fast)) else ExitTransition.None,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BrandLogo(Modifier.width(196.dp).aspectRatio(BRAND_LOGO_ASPECT))
                        Spacer(Modifier.height(24.dp))
                        BrandMascot(pose = MascotPose.Greeting, size = 196.dp)
                        Spacer(Modifier.height(36.dp))
                    }
                }
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}
