package com.example.data.telegram

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class TelegramUpdate(
    val updateId: Long,
    val messageId: Long,
    val chatId: String,
    val fromUsername: String,
    val text: String,
    val date: Long
)

data class TelegramBotInfo(
    val id: Long,
    val firstName: String,
    val username: String
)

class TelegramApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun getMe(botToken: String): Result<TelegramBotInfo> = withContext(Dispatchers.IO) {
        val token = botToken.trim()
        if (token.isBlank()) return@withContext Result.failure(IllegalArgumentException("Bot token is empty"))

        val url = "https://api.telegram.org/bot$token/getMe"
        val request = Request.Builder().url(url).get().build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorDesc = runCatching { JSONObject(body).optString("description") }.getOrNull()
                        ?: "HTTP ${response.code}"
                    return@withContext Result.failure(IOException(errorDesc))
                }
                val json = JSONObject(body)
                if (json.optBoolean("ok")) {
                    val result = json.getJSONObject("result")
                    val info = TelegramBotInfo(
                        id = result.getLong("id"),
                        firstName = result.optString("first_name", "Hermes Bot"),
                        username = result.optString("username", "")
                    )
                    Result.success(info)
                } else {
                    Result.failure(IOException(json.optString("description", "Failed to get bot info")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        botToken: String,
        chatId: String,
        text: String,
        parseMode: String = "Markdown"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val token = botToken.trim()
        val targetChat = chatId.trim()
        if (token.isBlank() || targetChat.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Bot token or Chat ID is empty"))
        }

        val url = "https://api.telegram.org/bot$token/sendMessage"
        val payload = JSONObject().apply {
            put("chat_id", targetChat)
            put("text", text)
            if (parseMode.isNotBlank()) {
                put("parse_mode", parseMode)
            }
            put("disable_web_page_preview", true)
        }

        val reqBody = payload.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder().url(url).post(reqBody).build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    // Try fallback without Markdown formatting if markdown parse failed
                    if (parseMode.isNotBlank()) {
                        return@withContext sendPlainMessage(token, targetChat, text)
                    }
                    val errorDesc = runCatching { JSONObject(body).optString("description") }.getOrNull()
                        ?: "HTTP ${response.code}"
                    return@withContext Result.failure(IOException(errorDesc))
                }
                val json = JSONObject(body)
                if (json.optBoolean("ok")) {
                    Result.success(true)
                } else {
                    Result.failure(IOException(json.optString("description", "Error sending message")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sendPlainMessage(botToken: String, chatId: String, text: String): Result<Boolean> {
        val url = "https://api.telegram.org/bot$botToken/sendMessage"
        val payload = JSONObject().apply {
            put("chat_id", chatId)
            put("text", text)
            put("disable_web_page_preview", true)
        }
        val reqBody = payload.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder().url(url).post(reqBody).build()
        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                if (json.optBoolean("ok")) Result.success(true)
                else Result.failure(IOException(json.optString("description", "Send error")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUpdates(
        botToken: String,
        offset: Long,
        timeoutSeconds: Int = 10
    ): Result<List<TelegramUpdate>> = withContext(Dispatchers.IO) {
        val token = botToken.trim()
        if (token.isBlank()) return@withContext Result.failure(IllegalArgumentException("Bot token is empty"))

        val url = "https://api.telegram.org/bot$token/getUpdates?offset=$offset&timeout=$timeoutSeconds"
        val request = Request.Builder().url(url).get().build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorDesc = runCatching { JSONObject(body).optString("description") }.getOrNull()
                        ?: "HTTP ${response.code}"
                    return@withContext Result.failure(IOException(errorDesc))
                }
                val json = JSONObject(body)
                if (!json.optBoolean("ok")) {
                    return@withContext Result.failure(IOException(json.optString("description", "Update error")))
                }

                val resultsArray = json.optJSONArray("result") ?: JSONArray()
                val list = mutableListOf<TelegramUpdate>()

                for (i in 0 until resultsArray.length()) {
                    val item = resultsArray.getJSONObject(i)
                    val updateId = item.getLong("update_id")
                    val message = item.optJSONObject("message") ?: item.optJSONObject("edited_message")
                    if (message != null) {
                        val messageId = message.optLong("message_id")
                        val chat = message.getJSONObject("chat")
                        val chatId = chat.opt("id")?.toString() ?: ""
                        val from = message.optJSONObject("from")
                        val fromUser = from?.optString("username", from.optString("first_name", "User")) ?: "User"
                        val text = message.optString("text", "")
                        val date = message.optLong("date", System.currentTimeMillis() / 1000)

                        if (text.isNotBlank()) {
                            list.add(
                                TelegramUpdate(
                                    updateId = updateId,
                                    messageId = messageId,
                                    chatId = chatId,
                                    fromUsername = fromUser,
                                    text = text,
                                    date = date
                                )
                            )
                        }
                    }
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
