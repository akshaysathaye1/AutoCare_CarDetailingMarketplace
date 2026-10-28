package com.example.mad_project_akshaysathaye_c049.data.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * AutoCareMessagingService receives FCM downstream messages dispatched from
 * secure backend Cloud Functions when booking documents change state.
 *
 * Security note: No server credentials or FCM server keys are stored in the client.
 * Tokens are registered to the authenticated customer's Firestore profile so that
 * backend triggers can target the correct device safely.
 */
class AutoCareMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM device token: $token")
        cacheToken(applicationContext, token)
        sendRegistrationTokenToServer(applicationContext, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")

        // 1. Process data payload if present
        if (remoteMessage.data.isNotEmpty()) {
            val bookingId = remoteMessage.data["bookingId"] ?: ""
            val status = remoteMessage.data["status"] ?: ""
            val garageName = remoteMessage.data["garageName"] ?: ""
            val serviceName = remoteMessage.data["serviceName"] ?: ""
            val vehicleDetails = remoteMessage.data["vehicleDetails"] ?: ""

            if (status.isNotBlank()) {
                NotificationHelper.showBookingStatusNotification(
                    context = applicationContext,
                    bookingId = bookingId,
                    status = status,
                    garageName = garageName,
                    serviceName = serviceName,
                    vehicleDetails = vehicleDetails
                )
                return
            }
        }

        // 2. Fallback to notification payload if present
        remoteMessage.notification?.let { notification ->
            val title = notification.title ?: "AutoCare Update"
            val body = notification.body ?: ""
            NotificationHelper.showBookingStatusNotification(
                context = applicationContext,
                bookingId = "",
                status = "confirmed",
                garageName = title,
                serviceName = body
            )
        }
    }

    // Meaningful Comment 31: Secure FCM device token caching and Firestore synchronization
    companion object {
        private const val TAG = "AutoCareMessaging"
        private const val PREFS_NAME = "autocare_fcm_prefs"
        private const val KEY_CACHED_TOKEN = "cached_fcm_token"

        fun cacheToken(context: Context, token: String) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString(KEY_CACHED_TOKEN, token).apply()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to cache FCM token", e)
            }
        }

        fun getCachedToken(context: Context): String? {
            return try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.getString(KEY_CACHED_TOKEN, null)
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Synchronizes the FCM device token with the authenticated user profile in Firestore.
         * Safe to call on application startup, user login, or token refresh.
         */
        fun syncDeviceToken(context: Context, explicitUid: String? = null) {
            val uid = explicitUid ?: FirebaseAuth.getInstance().currentUser?.uid ?: return
            val cached = getCachedToken(context)

            if (cached != null) {
                sendTokenToFirestore(uid, cached)
            } else {
                try {
                    FirebaseMessaging.getInstance().token
                        .addOnSuccessListener { token ->
                            cacheToken(context, token)
                            sendTokenToFirestore(uid, token)
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to fetch FCM token for synchronization", e)
                        }
                } catch (e: Exception) {
                    Log.w(TAG, "FirebaseMessaging service unavailable", e)
                }
            }
        }

        private fun sendRegistrationTokenToServer(context: Context, token: String) {
            val currentUser = FirebaseAuth.getInstance().currentUser ?: return
            sendTokenToFirestore(currentUser.uid, token)
        }

        private fun sendTokenToFirestore(uid: String, token: String) {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("users").document(uid)
                .update(
                    mapOf(
                        "fcmToken" to token,
                        "fcmTokenUpdatedAt" to System.currentTimeMillis()
                    )
                )
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token synced to user profile for UID: $uid")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to sync FCM token to user profile", e)
                }
        }
    }
}
