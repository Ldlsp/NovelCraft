package com.mozhou.novelcraft

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class AiGenerationForegroundService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskLabel = intent?.getStringExtra(EXTRA_TASK_LABEL).orEmpty().ifBlank { "创作" }
        val notification = notification(taskLabel)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(taskLabel: String): android.app.Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "AI 创作", NotificationManager.IMPORTANCE_LOW),
            )
        }
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("墨舟正在${taskLabel}")
            .setContentText("AI 创作会在后台继续，返回应用可查看进度")
            .setContentIntent(openApp)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "ai-generation"
        private const val NOTIFICATION_ID = 8_421
        private const val EXTRA_TASK_LABEL = "task_label"

        fun start(context: Context, taskLabel: String) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AiGenerationForegroundService::class.java)
                    .putExtra(EXTRA_TASK_LABEL, taskLabel),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AiGenerationForegroundService::class.java))
        }
    }
}
