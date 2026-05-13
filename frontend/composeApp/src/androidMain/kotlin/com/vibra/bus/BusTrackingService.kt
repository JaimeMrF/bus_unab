package com.vibra.bus

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
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
        private const val POLL_INTERVAL_MS = 15_000L
        private const val ARRIVAL_THRESHOLD_M = 200.0
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            appSettings.clearTracking()
            stopSelf()
            return START_NOT_STICKY
        }

        val plate    = intent?.getStringExtra(EXTRA_PLATE)    ?: return START_NOT_STICKY
        val stopLat  = intent.getDoubleExtra(EXTRA_STOP_LAT, 0.0)
        val stopLng  = intent.getDoubleExtra(EXTRA_STOP_LNG, 0.0)
        val stopName = intent.getStringExtra(EXTRA_STOP_NAME) ?: "tu parada"

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

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
        scope.cancel()
    }

    // ── Notification builders ─────────────────────────────────────────────────

    private fun buildTrackingNotif(contentText: String, etaMinutes: Int?): Notification {
        val tapIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP }
            ?: Intent()
        val tapPi = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, BusTrackingService::class.java).also {
            it.action = ACTION_STOP
        }
        val stopPi = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_BUS)
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
