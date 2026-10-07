package com.vibra.bus

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.vibra.bus.util.GoogleSignInManager
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val googleSignInManager: GoogleSignInManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        googleSignInManager.setActivity(this)

        // Sin permisos al abrir: ubicacion, camara y notificaciones se piden en contexto, con una
        // explicacion previa (ver presentation/permissions y el escaner).

        setContent {
            App()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        googleSignInManager.clearActivity()
    }
}
