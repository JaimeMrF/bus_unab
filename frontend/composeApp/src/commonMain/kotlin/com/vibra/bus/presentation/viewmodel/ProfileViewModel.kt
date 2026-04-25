package com.vibra.bus.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.vibra.bus.util.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ProfileData(
    val name: String,
    val email: String,
    val avatar: String,
    val role: String,
)

class ProfileViewModel(private val settings: AppSettings) : ViewModel() {

    private val _profile = MutableStateFlow(
        ProfileData(
            name = settings.userName,
            email = settings.userEmail,
            avatar = settings.userAvatar,
            role = settings.userRole,
        )
    )
    val profile: StateFlow<ProfileData> = _profile

    fun isDriver(): Boolean = settings.userRole == "driver"
}
