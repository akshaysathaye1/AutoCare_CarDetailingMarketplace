package com.example.mad_project_akshaysathaye_c049.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mad_project_akshaysathaye_c049.MainActivity
import com.example.mad_project_akshaysathaye_c049.R

/**
 * NotificationHelper manages the AutoCare system notification channel
 * and builds user-facing booking status update notifications.
 */
object NotificationHelper {

    const val CHANNEL_ID = "autocare_booking_updates"
    const val CHANNEL_NAME = "AutoCare Booking Updates"
    const val CHANNEL_DESCRIPTION = "Real-time status updates for customer detailing appointments"

    /**
     * Create the Notification Channel required on Android 8.0 (API 26) and higher.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Formats notification content for each real booking lifecycle state.
     * Returns Pair(title, body) or null if status does not warrant a notification.
     */
    fun formatNotificationContent(
        status: String,
        garageName: String,
        serviceName: String,
        vehicleDetails: String = ""
    ): Pair<String, String>? {
        val workshop = garageName.ifBlank { "AutoCare Partner Workshop" }
        val service = serviceName.ifBlank { "Detailing Service" }
        val vehicle = if (vehicleDetails.isNotBlank()) " for $vehicleDetails" else ""

        return when (status.lowercase()) {
            "confirmed" -> Pair(
                "Appointment Confirmed! 🚗",
                "$workshop has confirmed your appointment for $service$vehicle."
            )
            "in_progress" -> Pair(
                "Service Started 🛠️",
                "$workshop has started detailing work on your vehicle$vehicle."
            )
            "completed" -> Pair(
                "Service Completed! ✨",
                "Your vehicle is ready for pickup at $workshop. Tap to rate and review your experience."
            )
            "cancelled" -> Pair(
                "Booking Update ⚠️",
                "Your booking for $service at $workshop was cancelled."
            )
            else -> null
        }
    }

    /**
     * Post a notification on the device for an authenticated customer's booking status change.
     */
    fun showBookingStatusNotification(
        context: Context,
        bookingId: String,
        status: String,
        garageName: String,
        serviceName: String,
        vehicleDetails: String = ""
    ) {
        val content = formatNotificationContent(status, garageName, serviceName, vehicleDetails) ?: return
        val (title, body) = content

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("bookingId", bookingId)
            putExtra("route", "customer_bookings")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            bookingId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = (bookingId + status).hashCode()
            notificationManager.notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Notification permission might not be granted by user on Android 13+
        }
    }
}
