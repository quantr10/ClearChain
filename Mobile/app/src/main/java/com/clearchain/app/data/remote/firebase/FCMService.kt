package com.clearchain.app.data.remote.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.clearchain.app.MainActivity
import com.clearchain.app.R
import com.clearchain.app.data.local.SettingsStore
import com.clearchain.app.data.local.database.ClearChainDatabase
import com.clearchain.app.data.local.entity.NotificationEntity
import com.clearchain.app.domain.usecase.fcm.RegisterFCMTokenUseCase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Receives pushes and turns them into an inbox entry plus a system notification.
 *
 * The server sends data-only messages. A message carrying a `notification` block would be drawn
 * by the system while the app is backgrounded and [onMessageReceived] would never run, so the
 * notification could not be recorded — the inbox would be missing exactly the notifications the
 * user wasn't there to see. Data-only means this method always runs and the app builds the
 * system notification itself.
 */
@AndroidEntryPoint
class FCMService : FirebaseMessagingService() {

    @Inject
    lateinit var database: ClearChainDatabase

    @Inject
    lateinit var registerFCMTokenUseCase: RegisterFCMTokenUseCase

    @Inject
    lateinit var settingsStore: SettingsStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "FCMService"
        const val CHANNEL_ID = "clearchain_notifications"
        private const val CHANNEL_NAME = "ClearChain Notifications"
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🔔 FCM token rotated")

        // Saving locally is not enough — the server can only push to tokens it has been told
        // about, so the rotation has to be published or this device goes quiet.
        serviceScope.launch {
            registerFCMTokenUseCase.register(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        Log.d(TAG, "📩 Push received: ${data["type"]}")

        val title = data["title"] ?: message.notification?.title ?: "ClearChain"
        val body = data["body"] ?: message.notification?.body ?: ""
        val type = data["type"] ?: "general"

        serviceScope.launch {
            try {
                val (relatedId, relatedType) = deriveRelation(data)

                database.notificationDao().insert(
                    NotificationEntity(
                        // The server's row id, so the same notification arriving again over
                        // SignalR or an inbox sync replaces this row instead of duplicating it.
                        id = data["notificationId"] ?: message.messageId ?: UUID.randomUUID().toString(),
                        type = type,
                        title = title,
                        body = body,
                        relatedId = relatedId,
                        relatedType = relatedType,
                        isRead = false,
                        createdAt = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error saving notification", e)
            }

            // The inbox row above is written either way — a notification preference is about
            // being interrupted, not about losing the record, and a muted type the user later
            // goes looking for is still there. Only the tray notification is suppressed.
            if (settingsStore.allowsNotification(type)) {
                showNotification(title, body, data)
            } else {
                Log.d(TAG, "🔕 Suppressed by notification preference: $type")
            }
        }
    }

    /**
     * Mirrors the server's mapping from payload key to inbox relation, so a notification opened
     * from the tray deep-links to the same place as one opened from the inbox.
     */
    private fun deriveRelation(data: Map<String, String>): Pair<String?, String?> = when {
        data["requestId"] != null -> data["requestId"] to "pickup_request"
        data["listingId"] != null -> data["listingId"] to "listing"
        data["inventoryId"] != null -> data["inventoryId"] to "inventory"
        data["organizationId"] != null -> data["organizationId"] to "organization"
        else -> null to null
    }

    private fun showNotification(
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP

            data.forEach { (key, value) -> putExtra(key, value) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(), // Unique per notification so extras aren't reused
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(System.currentTimeMillis().toInt(), notification)

        Log.d(TAG, "🔔 Notification shown: $title")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pickup requests, listings and inventory updates"
            }

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}
