package com.vibra.bus.presentation.screens

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.TextButton
import androidx.compose.animation.AnimatedVisibility
import com.vibra.bus.data.api.normalizeServerUrl
import com.vibra.bus.data.api.ServerUrlResult
import com.vibra.bus.data.api.ALLOW_CLEARTEXT
import com.vibra.bus.data.api.BUILD_BASE_URL
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.vibra.bus.data.repository.BrandRepository
import com.vibra.bus.presentation.components.AppTextField
import com.vibra.bus.presentation.components.NeutralBadge
import com.vibra.bus.presentation.components.PrimaryButton
import com.vibra.bus.presentation.components.Staggered
import com.vibra.bus.util.ApiResult
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** Primer arranque sin organización: el usuario escribe el código (slug) de su transportadora. */
class OrganizationCodeScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository = koinInject<BrandRepository>()
        val scope = rememberCoroutineScope()

        var code by remember { mutableStateOf("") }
        var loading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }

        // Servidor avanzado: sin URL de build (p. ej. iOS) es obligatorio y se muestra abierto.
        val serverRequired = BUILD_BASE_URL.isBlank() && repository.serverUrl.isBlank()
        var showAdvanced by remember { mutableStateOf(serverRequired) }
        var server by remember { mutableStateOf(repository.serverUrl) }
        var serverError by remember { mutableStateOf<String?>(null) }

        fun submit() {
            if (loading || code.isBlank()) return
            error = null
            serverError = null
            var normalized: String? = null
            if (server.isNotBlank()) {
                when (val r = normalizeServerUrl(server, ALLOW_CLEARTEXT)) {
                    is ServerUrlResult.Valid -> normalized = r.url
                    is ServerUrlResult.Invalid -> {
                        showAdvanced = true
                        serverError = r.reason
                        return
                    }
                }
            } else if (serverRequired) {
                showAdvanced = true
                serverError = "Ingresa la dirección del servidor"
                return
            }
            loading = true
            scope.launch {
                when (val result = repository.selectOrganization(code, normalized)) {
                    is ApiResult.Success -> navigator.replaceAll(SplashScreen())
                    is ApiResult.HttpError -> error = result.message
                    is ApiResult.NetworkError -> error = "Sin conexión. Revisa tu internet e inténtalo de nuevo."
                }
                loading = false
            }
        }

        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 28.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Staggered(0) {
                        NeutralBadge(Modifier.size(96.dp), icon = Icons.Outlined.Apartment)
                    }
                    Spacer(Modifier.height(28.dp))
                    Staggered(1) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Tu organización",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Ingresa el código que te entregó tu transportadora para personalizar la app.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                    Staggered(2) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            AppTextField(
                                value = code,
                                onValueChange = { code = it; error = null },
                                label = "Código de organización",
                                error = error,
                                supportingText = "Ejemplo: mi-transportadora",
                                enabled = !loading,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(onGo = { submit() }),
                            )
                            TextButton(
                                onClick = { showAdvanced = !showAdvanced },
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(
                                    text = if (showAdvanced) "Ocultar opciones avanzadas" else "Opciones avanzadas",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                            AnimatedVisibility(visible = showAdvanced) {
                                AppTextField(
                                    value = server,
                                    onValueChange = { server = it; serverError = null },
                                    label = "Servidor",
                                    error = serverError,
                                    supportingText = "Ej.: api.miempresa.com (se agrega /api/v1)",
                                    enabled = !loading,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Uri,
                                        imeAction = ImeAction.Go,
                                    ),
                                    keyboardActions = KeyboardActions(onGo = { submit() }),
                                )
                            }
                            PrimaryButton(
                                text = "Continuar",
                                onClick = ::submit,
                                loading = loading,
                                enabled = code.isNotBlank(),
                            )
                        }
                    }
                }
            }
        }
    }
}
