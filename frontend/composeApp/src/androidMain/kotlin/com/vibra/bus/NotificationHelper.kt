package com.vibra.bus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

internal object NotificationHelper {

    const val CHANNEL_FOREGROUND = "vibra_bus_foreground"  // notificación persistente (silenciosa)
    const val CHANNEL_BUS   = "vibra_bus_tracking"         // alertas de llegada (con sonido)
    const val CHANNEL_ALERT = "vibra_bus_alerts"
    const val CHANNEL_INFO  = "vibra_general"

    private const val ID_APPROACHING = 101
    private const val ID_ARRIVED     = 102
    private const val ID_FULL        = 103
    private const val ID_GENERAL     = 104

    private val COLOR_GOLD = Color.parseColor("#E8A33D")
    private val COLOR_GREEN  = Color.parseColor("#2EBE6C")
    private val COLOR_ORANGE = Color.parseColor("#E8A33D")

    // ── Canales (llamar una sola vez al arrancar el servicio) ─────────────────

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return

        // Canal silencioso para la notificación fija del servicio en primer plano
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_FOREGROUND,
                "Bus en seguimiento",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación permanente mientras se sigue el bus"
                setShowBadge(false)
            }
        )

        // Canal con alerta para cuando el bus llega o se acerca
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_BUS,
                "Alertas de bus",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description      = "Alertas en tiempo real cuando tu bus se acerca o llega"
                enableLights(true)
                lightColor       = COLOR_GOLD
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 80, 200)
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERT,
                "Alertas del servicio",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Avisos sobre ocupación y estado del bus"
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_INFO,
                "Información general",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Noticias y anuncios de VibraBus"
            }
        )
    }

    // ── Punto de entrada ──────────────────────────────────────────────────────

    fun show(
        context : Context,
        type    : String,
        title   : String,
        body    : String,
        data    : Map<String, String> = emptyMap(),
    ) {
        when (type) {
            "bus_approaching" -> showApproaching(context, title, body, data)
            "bus_arrival"     -> showArrival(context, title, body)
            "bus_almost_full" -> showAlmostFull(context, title, body)
            else              -> showGeneral(context, title, body)
        }
    }

    // ── Bus se acerca ─────────────────────────────────────────────────────────

    private fun showApproaching(
        context : Context,
        title   : String,
        body    : String,
        data    : Map<String, String>,
    ) {
        val minutes  = data["minutes_away"]?.toIntOrNull() ?: 2
        val etaLabel = if (minutes == 1) "~1 min" else "~$minutes min"

        notify(
            context, ID_APPROACHING,
            base(context, CHANNEL_BUS)
                .setColor(COLOR_GOLD)
                .setColorized(true)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("$body\n¡Prepárate para abordar!")
                        .setSummaryText("VibraBus · $etaLabel para llegar")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                // Barra de progreso indeterminada mientras el bus viene en camino
                .setProgress(0, 0, true)
        )
    }

    // ── Bus llegó ─────────────────────────────────────────────────────────────

    private fun showArrival(context: Context, title: String, body: String) {
        notify(
            context, ID_ARRIVED,
            base(context, CHANNEL_BUS)
                .setColor(COLOR_GREEN)
                .setColorized(true)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(body)
                        .setSummaryText("VibraBus · ¡Aborda ahora!")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                // Progreso completo: barra llena en verde
                .setProgress(100, 100, false)
        )
    }

    // ── Bus casi lleno ────────────────────────────────────────────────────────

    private fun showAlmostFull(context: Context, title: String, body: String) {
        notify(
            context, ID_FULL,
            base(context, CHANNEL_ALERT)
                .setColor(COLOR_ORANGE)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
        )
    }

    // ── General / anuncio ─────────────────────────────────────────────────────

    private fun showGeneral(context: Context, title: String, body: String) {
        notify(
            context, ID_GENERAL,
            base(context, CHANNEL_INFO)
                .setColor(COLOR_GOLD)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_LOW)
        )
    }

    // ── Cancelar todas las notificaciones de seguimiento ─────────────────────

    fun cancelTrackingNotifications(context: Context) {
        val nm = NotificationManagerCompat.from(context)
        nm.cancel(ID_APPROACHING)
        nm.cancel(ID_ARRIVED)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun base(context: Context, channel: String): NotificationCompat.Builder {
        val tapIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP }
            ?: Intent()
        val tapPi = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.mipmap.ic_notification)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(tapPi)
    }

    private fun notify(context: Context, id: Int, builder: NotificationCompat.Builder) {
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS no concedido en API 33+: ignorar silenciosamente
        }
    }
}
