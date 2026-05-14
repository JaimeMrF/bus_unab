package com.vibra.bus

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.vibra.bus.data.repository.BusRepository
import com.vibra.bus.util.ApiResult
import com.vibra.bus.util.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class BusTrackingService : Service(), KoinComponent {

    private val busRepository: BusRepository by inject()
    private val appSettings: AppSettings by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollingJob: Job? = null

    companion object {
        const val ACTION_STOP     = "com.vibra.bus.STOP_TRACKING"
        const val EXTRA_PLATE     = "plate"
        const val EXTRA_STOP_LAT  = "stop_lat"
        const val EXTRA_STOP_LNG  = "stop_lng"
        const val EXTRA_STOP_NAME = "stop_name"
        const val NOTIF_ID = 200
        private const val POLL_INTERVAL_MS    = 15_000L
        private const val ARRIVAL_THRESHOLD_M = 200.0
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            appSettings.clearTracking()
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        // Cuando Android reinicia el servicio con START_STICKY el intent es null —
        // leemos los datos guardados en AppSettings como respaldo.
        val plate    = intent?.getStringExtra(EXTRA_PLATE)
                       ?: appSettings.trackingPlate.ifEmpty { null }
                       ?: run { stopSelf(); return START_NOT_STICKY }

        val stopLat  = intent?.getDoubleExtra(EXTRA_STOP_LAT, appSettings.trackingStopLat)
                       ?: appSettings.trackingStopLat
        val stopLng  = intent?.getDoubleExtra(EXTRA_STOP_LNG, appSettings.trackingStopLng)
                       ?: appSettings.trackingStopLng
        val stopName = intent?.getStringExtra(EXTRA_STOP_NAME)
                       ?: appSettings.trackingStopName.ifEmpty { "tu parada" }

        NotificationHelper.createChannels(this)
        startForeground(NOTIF_ID, buildTrackingNotif("Buscando tu bus...", null))

        pollingJob?.cancel()
        pollingJob = scope.launch {
            var arrivedNotified = false
            while (true) {
                try {
                    when (val result = busRepository.getBuses(stopLat, stopLng)) {
                        is ApiResult.Success -> {
                            val bus = result.data.data.find { it.plate == plate }
                            if (bus != null) {
                                val dist = haversine(bus.latitude, bus.longitude, stopLat, stopLng)
                                val eta  = max(1, (dist / (8.3 * 60)).roundToInt())

                                updateNotif(buildTrackingNotif("~$eta min · $stopName", eta))

                                if (dist < ARRIVAL_THRESHOLD_M && !arrivedNotified) {
                                    arrivedNotified = true
                                    NotificationHelper.show(
                                        this@BusTrackingService,
                                        "bus_arrival",
                                        "¡Tu bus está llegando!",
                                        "Prepara tu QR — $plate llega en menos de 1 minuto"
                                    )
                                }
                            }
                        }
                        else -> {}
                    }
                } catch (_: Exception) {}
                delay(POLL_INTERVAL_MS)
            }
        }

        // START_STICKY: si Android mata el proceso, lo reinicia automáticamente.
        // El intent puede ser null en el reinicio, pero leemos de AppSettings.
        return START_STICKY
    }

    // Cuando el usuario cierra la app desde el panel de recientes, Android llama
    // onTaskRemoved(). Si hay tracking activo, programamos un reinicio del servicio.
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (!appSettings.hasActiveTracking()) return

        val restartIntent = Intent(this, BusTrackingService::class.java).apply {
            putExtra(EXTRA_PLATE,     appSettings.trackingPlate)
            putExtra(EXTRA_STOP_LAT,  appSettings.trackingStopLat)
            putExtra(EXTRA_STOP_LNG,  appSettings.trackingStopLng)
            putExtra(EXTRA_STOP_NAME, appSettings.trackingStopName)
        }
        val pi = PendingIntent.getService(
            this, 2, restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alarmManager = getSystemService(ALARM_SERVICE) as? AlarmManager
        // Reinicio no exacto: funciona sin permisos adicionales en Android 12+
        alarmManager?.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 2_000L,
            pi,
        )
    }

    override fun onDestroy() {
        pollingJob?.cancel()
        scope.cancel()
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        getSystemService(NotificationManager::class.java)?.cancel(NOTIF_ID)
        NotificationHelper.cancelTrackingNotifications(this)
        super.onDestroy()
    }

    // ── Notification builders ─────────────────────────────────────────────────

    private fun buildTrackingNotif(contentText: String, etaMinutes: Int?): Notification {
        val tapIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP }
            ?: Intent()
        val tapPi = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = Intent(this, BusTrackingService::class.java).also {
            it.action = ACTION_STOP
        }
        val stopPi = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Usamos CHANNEL_FOREGROUND (IMPORTANCE_LOW) para que la notificación
        // sea silenciosa y permanente, sin interrumpir al usuario.
        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_FOREGROUND)
            .setSmallIcon(R.mipmap.ic_notification)
            .setContentTitle("VibraBus — Siguiendo tu bus")
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setContentIntent(tapPi)
            .addAction(0, "Cancelar seguimiento", stopPi)
            .apply {
                if (etaMinutes != null) {
                    val progress = (30 - etaMinutes.coerceAtMost(30)).coerceAtLeast(0)
                    setProgress(30, progress, false)
                } else {
                    setProgress(0, 0, true)
                }
            }
            .build()
    }

    private fun updateNotif(notification: Notification) {
        val nm = getSystemService(NotificationManager::class.java)
        nm?.notify(NOTIF_ID, notification)
    }

    // ── Math ──────────────────────────────────────────────────────────────────

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r  = 6371e3
        val p1 = lat1 * PI / 180
        val p2 = lat2 * PI / 180
        val dp = (lat2 - lat1) * PI / 180
        val dl = (lon2 - lon1) * PI / 180
        val a  = sin(dp / 2).pow(2) + cos(p1) * cos(p2) * sin(dl / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
