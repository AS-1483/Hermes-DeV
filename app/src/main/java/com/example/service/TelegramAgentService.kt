package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.repository.ApiProviderRepository
import com.example.data.telegram.TelegramApiService
import com.example.data.telegram.TelegramPreferencesRepository
import com.example.domain.engine.AiClientManager
import com.example.domain.engine.HermesAgentEngine
import com.example.domain.engine.ShellExecutionEngine
import com.example.domain.telegram.TelegramBotController

class TelegramAgentService : Service() {

    private lateinit var telegramRepo: TelegramPreferencesRepository
    private lateinit var telegramApi: TelegramApiService
    private lateinit var botController: TelegramBotController

    companion object {
        const val CHANNEL_ID = "hermes_telegram_service_channel"
        const val NOTIFICATION_ID = 9001

        const val ACTION_START = "com.example.service.START_TELEGRAM_BOT"
        const val ACTION_STOP = "com.example.service.STOP_TELEGRAM_BOT"

        fun startService(context: Context) {
            val intent = Intent(context, TelegramAgentService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TelegramAgentService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(applicationContext)
        val shellEngine = ShellExecutionEngine(applicationContext, db.taskDao())
        val aiClientManager = AiClientManager(db.apiProviderDao())
        val agentEngine = HermesAgentEngine(
            context = applicationContext,
            taskDao = db.taskDao(),
            chatDao = db.chatDao(),
            artifactDao = db.artifactDao(),
            skillDao = db.skillDao(),
            aiClientManager = aiClientManager,
            shellExecutionEngine = shellEngine
        )

        telegramRepo = TelegramPreferencesRepository(applicationContext)
        telegramApi = TelegramApiService()
        botController = TelegramBotController(
            context = applicationContext,
            telegramRepo = telegramRepo,
            telegramApi = telegramApi,
            agentEngine = agentEngine,
            shellEngine = shellEngine,
            db = db
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                botController.stopPolling()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, createNotification())
                botController.startPolling()
            }
        }
        return START_STICKY
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hermes Telegram Bot Controller",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Hermes Dev Telegram Bot listener running in background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Hermes Dev Agent — Telegram Bot Active")
            .setContentText("Listening for remote commands & autonomous tasks")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        botController.stopPolling()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
