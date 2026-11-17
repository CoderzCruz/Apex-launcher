package net.kdt.pojavlaunch.aerix.bridge

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import net.kdt.pojavlaunch.LauncherActivity
import net.kdt.pojavlaunch.R

/**
 * Heads-up "Update now" notification for a published Vortex release: it stays in the
 * shade after the app closes, and tapping it opens the launcher where the update
 * dialog (and its auto-download) takes over. No-op when POST_NOTIFICATIONS is denied.
 */
object VortexUpdateNotification {
    private const val CHANNEL_ID = "vortex_updates"
    private const val NOTIFICATION_ID = 940001

    fun show(context: Context, versionCode: Long) {
        val app = context.applicationContext
        runCatching {
            val nm = app.getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= 26 && nm != null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Vortex updates",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "New Vortex Launcher builds are ready to install"
                    }
                )
            }
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) return@runCatching

            val intent = Intent(app, LauncherActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val pending = PendingIntent.getActivity(
                app, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(app, CHANNEL_ID)
                .setSmallIcon(R.drawable.notif_icon)
                .setContentTitle("Vortex update ready")
                .setContentText("versionCode $versionCode available — tap to update now")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        "Vortex Launcher update $versionCode is ready to install. " +
                            "Tap to open Vortex — the download starts automatically."
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()
            nm?.notify(NOTIFICATION_ID, notification)
        }
    }

    fun cancel(context: Context) {
        runCatching {
            context.applicationContext.getSystemService(NotificationManager::class.java)
                ?.cancel(NOTIFICATION_ID)
        }
    }
}
