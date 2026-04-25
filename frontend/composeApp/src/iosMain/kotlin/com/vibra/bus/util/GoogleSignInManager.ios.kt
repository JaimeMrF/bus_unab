package com.vibra.bus.util

actual class GoogleSignInManager {

    actual suspend fun signIn(): String? {
        // iOS Google Sign-In implementation via GoogleSignIn-iOS SDK
        // In production: use GIDSignIn.sharedInstance.signIn(...)
        // and extract user.idToken?.tokenString
        return null
    }

    actual fun signOut() {
        // GIDSignIn.sharedInstance.signOut()
    }
}
