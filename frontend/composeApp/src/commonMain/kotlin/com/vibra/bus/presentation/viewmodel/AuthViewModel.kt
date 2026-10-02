package com.vibra.bus.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibra.bus.data.model.UserDto
import com.vibra.bus.data.repository.AuthRepository
import com.vibra.bus.util.ApiResult
import com.vibra.bus.util.AppSettings
import com.vibra.bus.util.DEVICE_PLATFORM
import com.vibra.bus.util.GoogleSignInManager
import com.vibra.bus.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthEvent {
    data object NavigateToHome : AuthEvent()
    data object NavigateToLogin : AuthEvent()
    data class ShowError(val message: String) : AuthEvent()
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val settings: AppSettings,
    private val googleSignInManager: GoogleSignInManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<UserDto>>(UiState.Idle)
    val uiState: StateFlow<UiState<UserDto>> = _uiState

    private val _event = MutableStateFlow<AuthEvent?>(null)
    val event: StateFlow<AuthEvent?> = _event

    fun checkSession() {
        if (!authRepository.isLoggedIn()) {
            _event.value = AuthEvent.NavigateToLogin
            return
        }
        // Sesion optimista: con token guardado se entra de inmediato (sin esperar a la red) y se
        // valida en segundo plano. Si el token ya no sirve (401), el polling de Home detecta la
        // sesion expirada y vuelve al login; aqui ademas se limpia la sesion local.
        _event.value = AuthEvent.NavigateToHome
        viewModelScope.launch {
            when (val result = authRepository.getMe()) {
                is ApiResult.HttpError -> if (result.code == 401) settings.clearSession()
                else -> Unit
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _event.value = AuthEvent.ShowError("Por favor completa todos los campos")
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = authRepository.login(email, password)) {
                is ApiResult.Success -> {
                    val response = result.data
                    val userData = response.data?.user
                    if (response.success && userData != null) {
                        registerFcmToken()
                        _uiState.value = UiState.Success(userData)
                        _event.value = AuthEvent.NavigateToHome
                    } else {
                        val errorMsg = response.message ?: "Error de credenciales"
                        _uiState.value = UiState.Error(errorMsg)
                        _event.value = AuthEvent.ShowError(errorMsg)
                    }
                }
                is ApiResult.HttpError -> {
                    _uiState.value = UiState.Error(result.message)
                    _event.value = AuthEvent.ShowError(result.message)
                }
                is ApiResult.NetworkError -> {
                    _uiState.value = UiState.Error("Sin conexión a internet")
                    _event.value = AuthEvent.ShowError("Sin conexión a internet")
                }
            }
        }
    }

    fun loginWithGoogle() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val idToken = googleSignInManager.signIn()
                if (idToken != null) {
                    when (val result = authRepository.loginWithGoogle(idToken)) {
                        is ApiResult.Success -> {
                            val response = result.data
                            val userData = response.data?.user
                            if (response.success && userData != null) {
                                registerFcmToken()
                                _uiState.value = UiState.Success(userData)
                                _event.value = AuthEvent.NavigateToHome
                            } else {
                                val errorMsg = response.message ?: "Error con Google"
                                _uiState.value = UiState.Error(errorMsg)
                                _event.value = AuthEvent.ShowError(errorMsg)
                            }
                        }
                        is ApiResult.HttpError -> {
                            _uiState.value = UiState.Error(result.message)
                            _event.value = AuthEvent.ShowError(result.message)
                        }
                        is ApiResult.NetworkError -> {
                            _uiState.value = UiState.Error("Sin conexión a internet")
                            _event.value = AuthEvent.ShowError("Sin conexión a internet")
                        }
                    }
                } else {
                    _uiState.value = UiState.Idle
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Error desconocido con Google"
                _uiState.value = UiState.Error(errorMsg)
                _event.value = AuthEvent.ShowError(errorMsg)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            val fcmToken = settings.fcmToken
            if (fcmToken.isNotEmpty()) {
                authRepository.deleteDeviceToken(fcmToken, DEVICE_PLATFORM)
            }
            authRepository.logout()
            googleSignInManager.signOut()
            _event.value = AuthEvent.NavigateToLogin
        }
    }

    private suspend fun registerFcmToken() {
        val fcmToken = settings.fcmToken
        if (fcmToken.isNotEmpty()) {
            authRepository.registerDeviceToken(fcmToken, DEVICE_PLATFORM)
        }
    }

    fun consumeEvent() {
        _event.value = null
    }
}
