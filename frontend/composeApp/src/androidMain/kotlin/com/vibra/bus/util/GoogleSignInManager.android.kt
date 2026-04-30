package com.vibra.bus.util

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.vibra.bus.BuildConfig
import java.lang.ref.WeakReference

actual class GoogleSignInManager(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)
    private var activityRef: WeakReference<ComponentActivity>? = null

    fun setActivity(activity: ComponentActivity) {
        activityRef = WeakReference(activity)
    }

    fun clearActivity() {
        activityRef = null
    }

    actual suspend fun signIn(): String? {
        val activity = activityRef?.get() ?: return null
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(BuildConfig.GOOGLE_SERVER_CLIENT_ID)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activity,
            )

            GoogleIdTokenCredential.createFrom(result.credential.data).idToken
        } catch (e: GetCredentialException) {
            println("GoogleSignIn Error: ${e.message}")
            null
        } catch (e: Exception) {
            println("GoogleSignIn Unknown Error: ${e.message}")
            null
        }
    }

    actual fun signOut() {
        clearActivity()
    }
}
