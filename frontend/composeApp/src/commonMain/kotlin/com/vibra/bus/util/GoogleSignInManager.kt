package com.vibra.bus.util

expect class GoogleSignInManager {
    suspend fun signIn(): String?
    fun signOut()
}
