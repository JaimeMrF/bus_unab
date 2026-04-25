package com.vibra.bus.data.repository

import com.vibra.bus.data.api.AuthApi
import com.vibra.bus.data.model.AuthResponse
import com.vibra.bus.data.model.BasicResponse
import com.vibra.bus.data.model.LoginRequest
import com.vibra.bus.data.model.MeResponse
import com.vibra.bus.util.ApiResult
import com.vibra.bus.util.AppSettings

class AuthRepository(
    private val api: AuthApi,
    private val settings: AppSettings,
) {
    suspend fun login(email: String, password: String): ApiResult<AuthResponse> {
        return api.login(LoginRequest(email, password)).also { result ->
            if (result is ApiResult.Success) {
                saveSession(result.data)
            }
        }
    }

    suspend fun loginWithGoogle(idToken: String): ApiResult<AuthResponse> {
        return api.loginWithGoogle(idToken).also { result ->
            if (result is ApiResult.Success) {
                saveSession(result.data)
            }
        }
    }

    private fun saveSession(response: AuthResponse) {
        val d = response.data ?: return
        settings.token = d.token
        settings.userRole = d.user.role
        settings.userId = d.user.id
        settings.userName = d.user.name
        settings.userEmail = d.user.email
        settings.userAvatar = d.user.avatar ?: ""
    }

    suspend fun logout(): ApiResult<BasicResponse> {
        return api.logout().also {
            settings.clearSession()
        }
    }

    suspend fun getMe(): ApiResult<MeResponse> = api.getMe()

    suspend fun registerDeviceToken(fcmToken: String): ApiResult<BasicResponse> =
        api.registerDeviceToken(fcmToken)

    suspend fun deleteDeviceToken(fcmToken: String): ApiResult<BasicResponse> =
        api.deleteDeviceToken(fcmToken)

    fun isLoggedIn(): Boolean = settings.isLoggedIn()
    fun getUserRole(): String = settings.userRole
    fun getUserId(): Int = settings.userId
    fun getUserName(): String = settings.userName
    fun getUserEmail(): String = settings.userEmail
    fun getUserAvatar(): String = settings.userAvatar
}
