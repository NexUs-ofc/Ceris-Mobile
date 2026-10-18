package com.example.ceris.notification

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.ceris.R
import com.example.ceris.local.SessionManager
import com.example.ceris.view.MainActivity

class PushNotificationPublisher(private val context: Context, private val state: PushState) {
    fun permitted(): Boolean {
        val runtimePermission =
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return runtimePermission && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun publish(message: PushMessage): Boolean {
        if (!permitted()) return false
        val generation = SessionManager(context).snapshot()?.generation ?: return false
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        if (Build.VERSION.SDK_INT >= 26 &&
            manager.getNotificationChannel(message.channel)?.importance == NotificationManager.IMPORTANCE_NONE
        ) {
            return false
        }
        val intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                data =
                    Uri.Builder().scheme("ceris").authority("notification")
                        .appendPath(message.profileId.toString()).appendPath(message.notificationId).build()
            }
        val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val publicVersion =
            NotificationCompat.Builder(context, message.channel)
                .setSmallIcon(R.drawable.ic_menu_bell).setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.notificacao_privada)).build()
        val notification =
            NotificationCompat.Builder(context, message.channel)
                .setSmallIcon(R.drawable.ic_menu_bell)
                .setContentTitle(message.title).setContentText(message.message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message.message))
                .setContentIntent(pending).setAutoCancel(true).setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(publicVersion)
                .build()
        return try {
            manager.notify("$TAG$generation:${message.profileId}:${message.notificationId}", 1, notification)
            true
        } catch (_: SecurityException) {
            // A permissao pode ser revogada entre a verificacao e o notify.
            false
        }
    }

    fun clear(keepGeneration: String? = null) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val active =
            manager.activeNotifications.filter {
                it.tag?.startsWith(TAG) == true &&
                    (keepGeneration == null || it.tag?.startsWith("$TAG$keepGeneration:") != true)
            }
        // Inclui avisos aceitos pelo notify que o sistema ainda esteja enfileirando.
        val known = state.publicationKeys().filter { keepGeneration == null || !it.startsWith("$keepGeneration:") }
        val tags = active.mapNotNull { it.tag } + known.map { "$TAG$it" }
        tags.distinct().forEach { manager.cancel(it, 1) }
    }

    private companion object {
        const val TAG = "ceris.notify:"
    }
}
