package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    const val CHANNEL_TASKS = "hermes_tasks"
    const val CHANNEL_APPROVALS = "hermes_approvals"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val taskChannel = NotificationChannel(
                CHANNEL_TASKS,
                "Hermes Task Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when background code generation, builds, or tests complete."
            }

            val approvalChannel = NotificationChannel(
                CHANNEL_APPROVALS,
                "Security Approvals",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts when a command requires user authorization."
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(taskChannel)
            manager.createNotificationChannel(approvalChannel)
        }
    }

    fun sendTaskNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = 1001
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_TASKS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Handled when permission is not granted
        }
    }

    fun sendApprovalNotification(
        context: Context,
        command: String
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_APPROVALS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Security Approval Required")
            .setContentText("Hermes paused for command: $command")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Hermes paused: $command. Tap to authorize or reject."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(2001, builder.build())
        } catch (_: SecurityException) {
            // Handled
        }
    }
}
