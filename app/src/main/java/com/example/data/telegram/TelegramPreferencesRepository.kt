package com.example.data.telegram

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.telegramDataStore by preferencesDataStore(name = "hermes_telegram_prefs")

data class TelegramConfig(
    val botToken: String = "",
    val chatId: String = "",
    val isEnabled: Boolean = false,
    val notifyOnTaskComplete: Boolean = true,
    val notifyOnApprovalNeeded: Boolean = true,
    val allowRemoteExecution: Boolean = true,
    val lastUpdateId: Long = 0L,
    val lastSyncTime: Long = 0L,
    val lastStatusMessage: String = "Not Configured"
)

class TelegramPreferencesRepository(private val context: Context) {

    private object Keys {
        val BOT_TOKEN = stringPreferencesKey("telegram_bot_token")
        val CHAT_ID = stringPreferencesKey("telegram_chat_id")
        val IS_ENABLED = booleanPreferencesKey("telegram_is_enabled")
        val NOTIFY_TASK_COMPLETE = booleanPreferencesKey("telegram_notify_task_complete")
        val NOTIFY_APPROVAL = booleanPreferencesKey("telegram_notify_approval")
        val ALLOW_REMOTE_EXEC = booleanPreferencesKey("telegram_allow_remote_exec")
        val LAST_UPDATE_ID = longPreferencesKey("telegram_last_update_id")
        val LAST_SYNC_TIME = longPreferencesKey("telegram_last_sync_time")
        val LAST_STATUS_MESSAGE = stringPreferencesKey("telegram_last_status_message")
    }

    val configFlow: Flow<TelegramConfig> = context.telegramDataStore.data.map { prefs ->
        TelegramConfig(
            botToken = prefs[Keys.BOT_TOKEN] ?: "",
            chatId = prefs[Keys.CHAT_ID] ?: "",
            isEnabled = prefs[Keys.IS_ENABLED] ?: false,
            notifyOnTaskComplete = prefs[Keys.NOTIFY_TASK_COMPLETE] ?: true,
            notifyOnApprovalNeeded = prefs[Keys.NOTIFY_APPROVAL] ?: true,
            allowRemoteExecution = prefs[Keys.ALLOW_REMOTE_EXEC] ?: true,
            lastUpdateId = prefs[Keys.LAST_UPDATE_ID] ?: 0L,
            lastSyncTime = prefs[Keys.LAST_SYNC_TIME] ?: 0L,
            lastStatusMessage = prefs[Keys.LAST_STATUS_MESSAGE] ?: "Not Configured"
        )
    }

    suspend fun saveConfig(
        botToken: String,
        chatId: String,
        isEnabled: Boolean,
        notifyOnTaskComplete: Boolean = true,
        notifyOnApprovalNeeded: Boolean = true,
        allowRemoteExecution: Boolean = true
    ) {
        context.telegramDataStore.edit { prefs ->
            prefs[Keys.BOT_TOKEN] = botToken.trim()
            prefs[Keys.CHAT_ID] = chatId.trim()
            prefs[Keys.IS_ENABLED] = isEnabled
            prefs[Keys.NOTIFY_TASK_COMPLETE] = notifyOnTaskComplete
            prefs[Keys.NOTIFY_APPROVAL] = notifyOnApprovalNeeded
            prefs[Keys.ALLOW_REMOTE_EXEC] = allowRemoteExecution
            if (botToken.isNotBlank() && chatId.isNotBlank() && isEnabled) {
                prefs[Keys.LAST_STATUS_MESSAGE] = "Configured & Active"
            } else if (!isEnabled) {
                prefs[Keys.LAST_STATUS_MESSAGE] = "Disabled by user"
            } else {
                prefs[Keys.LAST_STATUS_MESSAGE] = "Missing Token or Chat ID"
            }
        }
    }

    suspend fun updateLastUpdateId(updateId: Long) {
        context.telegramDataStore.edit { prefs ->
            prefs[Keys.LAST_UPDATE_ID] = updateId
            prefs[Keys.LAST_SYNC_TIME] = System.currentTimeMillis()
        }
    }

    suspend fun updateStatusMessage(status: String) {
        context.telegramDataStore.edit { prefs ->
            prefs[Keys.LAST_STATUS_MESSAGE] = status
            prefs[Keys.LAST_SYNC_TIME] = System.currentTimeMillis()
        }
    }

    suspend fun toggleEnabled(enabled: Boolean) {
        context.telegramDataStore.edit { prefs ->
            prefs[Keys.IS_ENABLED] = enabled
            prefs[Keys.LAST_STATUS_MESSAGE] = if (enabled) "Polling Active" else "Paused"
        }
    }
}
