package com.vibra.bus.presentation.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.vibra.bus.presentation.components.PrimaryButton
import java.util.concurrent.Executors

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
actual fun QRScannerView(modifier: Modifier, onResult: (String) -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = context as? LifecycleOwner

    fun hasPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    var granted by remember { mutableStateOf(hasPermission()) }
    var asked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        asked = true
    }

    // Al volver de Ajustes se vuelve a evaluar el permiso.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = hasPermission()
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { lifecycleOwner?.lifecycle?.removeObserver(observer) }
    }

    if (!granted) {
        val permanent = asked && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
        CameraPermissionPrompt(
            modifier = modifier,
            permanentlyDenied = permanent,
            onRequest = { launcher.launch(Manifest.permission.CAMERA) },
            onOpenSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
        )
        return
    }

    val executor = remember { Executors.newSingleThreadExecutor() }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )
    }

    // Libera la camara y el analizador al salir de la pantalla (si no, sigue abierta con la actividad).
    DisposableEffect(Unit) {
        onDispose {
            camera?.cameraControl?.enableTorch(false)
            cameraProvider?.unbindAll()
            scanner.close()
            executor.shutdown()
        }
    }
    LaunchedEffect(torchOn, camera) { camera?.cameraControl?.enableTorch(torchOn) }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    val provider = future.get()
                    cameraProvider = provider
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(executor) { imageProxy ->
                        processImage(imageProxy, scanner) { value -> onResult(value) }
                    }
                    provider.unbindAll()
                    camera = provider.bindToLifecycle(
                        ctx as LifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize(),
        )
        // Marco de lectura
        Canvas(modifier = Modifier.size(240.dp).align(Alignment.Center)) {
            drawRect(color = Color.White, style = Stroke(width = 4f))
        }
        // Linterna (solo si el dispositivo tiene flash)
        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            IconButton(
                onClick = { torchOn = !torchOn },
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            ) {
                Icon(
                    imageVector = if (torchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    contentDescription = if (torchOn) "Apagar linterna" else "Encender linterna",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionPrompt(
    modifier: Modifier,
    permanentlyDenied: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (permanentlyDenied)
                "El acceso a la cámara está bloqueado. Actívalo en Ajustes para escanear códigos QR."
            else
                "Necesitamos la cámara para escanear códigos QR.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            text = if (permanentlyDenied) "Abrir ajustes" else "Permitir cámara",
            onClick = if (permanentlyDenied) onOpenSettings else onRequest,
        )
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImage(
    imageProxy: ImageProxy,
    scanner: BarcodeScanner,
    onResult: (String) -> Unit,
) {
    val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    scanner.process(image)
        .addOnSuccessListener { barcodes ->
            barcodes.firstOrNull()?.rawValue?.let { onResult(it) }
        }
        .addOnCompleteListener { imageProxy.close() }
}
