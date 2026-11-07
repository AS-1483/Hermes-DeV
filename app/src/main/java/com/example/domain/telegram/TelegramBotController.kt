package com.example.domain.telegram

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.telegram.TelegramApiService
import com.example.data.telegram.TelegramConfig
import com.example.data.telegram.TelegramPreferencesRepository
import com.example.data.telegram.TelegramUpdate
import com.example.domain.engine.HermesAgentEngine
import com.example.domain.engine.ShellExecutionEngine
import com.example.domain.model.VerificationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class TelegramBotController(
    private val context: Context,
    private val telegramRepo: TelegramPreferencesRepository,
    private val telegramApi: TelegramApiService,
    private val agentEngine: HermesAgentEngine,
    private val shellEngine: ShellExecutionEngine,
    private val db: AppDatabase
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var pollingJob: Job? = null

    fun startPolling() {
        if (pollingJob?.isActive == true) return

        pollingJob = scope.launch {
            while (isActive) {
                try {
                    val config = telegramRepo.configFlow.first()
                    if (!config.isEnabled || config.botToken.isBlank() || config.chatId.isBlank()) {
                        delay(5000)
                        continue
                    }

                    val offset = config.lastUpdateId + 1
                    val updatesResult = telegramApi.getUpdates(
                        botToken = config.botToken,
                        offset = offset,
                        timeoutSeconds = 8
                    )

                    updatesResult.onSuccess { updates ->
                        var maxUpdateId = config.lastUpdateId
                        for (update in updates) {
                            if (update.updateId > maxUpdateId) {
                                maxUpdateId = update.updateId
                            }

                            // Security check: Only process messages from the authorized chatId
                            if (update.chatId.trim() == config.chatId.trim()) {
                                handleIncomingTelegramCommand(config, update)
                            }
                        }

                        if (maxUpdateId > config.lastUpdateId) {
                            telegramRepo.updateLastUpdateId(maxUpdateId)
                        }
                    }.onFailure { err ->
                        telegramRepo.updateStatusMessage("Polling Warning: ${err.message ?: "Network error"}")
                        delay(4000)
                    }

                    delay(1500)
                } catch (e: Exception) {
                    delay(5000)
                }
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun handleIncomingTelegramCommand(
        config: TelegramConfig,
        update: TelegramUpdate
    ) {
        val rawText = update.text.trim()
        if (rawText.isBlank()) return

        when {
            rawText.startsWith("/start") || rawText.startsWith("/help") -> {
                sendHelpMessage(config)
            }
            rawText.startsWith("/status") -> {
                sendStatusMessage(config)
            }
            rawText.startsWith("/projects") -> {
                sendProjectsMessage(config)
            }
            rawText.startsWith("/sessions") -> {
                sendSessionsMessage(config)
            }
            rawText.startsWith("/workspace") -> {
                sendWorkspaceFilesMessage(config)
            }
            rawText.startsWith("/cat ") -> {
                val filePath = rawText.removePrefix("/cat ").trim()
                sendCatFileMessage(config, filePath)
            }
            rawText.startsWith("/sh ") -> {
                if (!config.allowRemoteExecution) {
                    telegramApi.sendMessage(
                        botToken = config.botToken,
                        chatId = config.chatId,
                        text = "⚠️ Remote command execution is disabled in Hermes Dev settings."
                    )
                    return
                }
                val cmd = rawText.removePrefix("/sh ").trim()
                executeDirectShellCommand(config, cmd)
            }
            rawText.startsWith("/approve") -> {
                handleApproveCommand(config)
            }
            rawText.startsWith("/ask ") || rawText.startsWith("/run ") || !rawText.startsWith("/") -> {
                val prompt = when {
                    rawText.startsWith("/ask ") -> rawText.removePrefix("/ask ").trim()
                    rawText.startsWith("/run ") -> rawText.removePrefix("/run ").trim()
                    else -> rawText
                }
                executeAgentPrompt(config, prompt)
            }
        }
    }

    private suspend fun sendHelpMessage(config: TelegramConfig) {
        val helpText = """
            🤖 *Hermes Dev Agent — Telegram Remote Control*
            
            You have full interactive control of the Android Hermes Dev Agent:
            
            • *Direct Chat / Task:* Send any prompt directly (e.g. `Build a REST API in Kotlin`)
            • `/run <prompt>` or `/ask <prompt>` — Autonomous Hermes pipeline run
            • `/sh <command>` — Execute shell command in active workspace
            • `/status` — View active project, session & execution status
            • `/projects` — List all development projects
            • `/sessions` — List sessions in active project
            • `/workspace` — List workspace files & directory tree
            • `/cat <file>` — Read file content from workspace
            • `/approve` — Approve pending sensitive command
            • `/help` — Display this command menu
            
            ⚡ _Powered by Hermes Dev Agent local & multi-provider engine_
        """.trimIndent()

        telegramApi.sendMessage(
            botToken = config.botToken,
            chatId = config.chatId,
            text = helpText
        )
    }

    private suspend fun sendStatusMessage(config: TelegramConfig) {
        val projects = db.projectDao().getAllProjectsSync()
        val activeProj = projects.firstOrNull()
        val activeProjName = activeProj?.name ?: "Default Workspace"
        val activeDomain = activeProj?.domain ?: "BACKEND_API"
        val pendingTask = db.taskDao().getLatestTaskSync()

        val statusText = """
            📊 *Hermes Dev Agent Status Report*
            
            • *Active Project:* `$activeProjName`
            • *Domain:* `$activeDomain`
            • *Remote Execution:* `${if (config.allowRemoteExecution) "Enabled" else "Disabled"}`
            • *Latest Task Status:* `${pendingTask?.status ?: "IDLE"}`
            • *Current Step:* `${pendingTask?.currentStepName ?: "Ready"}`
            • *Last Update ID:* `${config.lastUpdateId}`
        """.trimIndent()

        telegramApi.sendMessage(
            botToken = config.botToken,
            chatId = config.chatId,
            text = statusText
        )
    }

    private suspend fun sendProjectsMessage(config: TelegramConfig) {
        val projects = db.projectDao().getAllProjectsSync()
        if (projects.isEmpty()) {
            telegramApi.sendMessage(
                botToken = config.botToken,
                chatId = config.chatId,
                text = "📁 No projects registered yet in Hermes Dev."
            )
            return
        }

        val text = buildString {
            appendLine("📁 *Registered Hermes Projects (${projects.size}):*")
            projects.forEachIndexed { i, p ->
                appendLine("${i + 1}. *${p.name}* (`${p.domain}`)")
                if (p.description.isNotBlank()) {
                    appendLine("   _${p.description}_")
                }
            }
        }

        telegramApi.sendMessage(
            botToken = config.botToken,
            chatId = config.chatId,
            text = text
        )
    }

    private suspend fun sendSessionsMessage(config: TelegramConfig) {
        val projects = db.projectDao().getAllProjectsSync()
        val activeProj = projects.firstOrNull()
        if (activeProj == null) {
            telegramApi.sendMessage(
                botToken = config.botToken,
                chatId = config.chatId,
                text = "No active project found."
            )
            return
        }

        val sessions = db.chatDao().getSessionsForProjectSync(activeProj.id)
        val text = buildString {
            appendLine("💬 *Sessions for ${activeProj.name} (${sessions.size}):*")
            sessions.forEachIndexed { i, s ->
                appendLine("${i + 1}. *${s.title}* (`${s.id.take(8)}...`)")
            }
        }

        telegramApi.sendMessage(
            botToken = config.botToken,
            chatId = config.chatId,
            text = text
        )
    }

    private suspend fun sendWorkspaceFilesMessage(config: TelegramConfig) {
        val projects = db.projectDao().getAllProjectsSync()
        val activeProj = projects.firstOrNull() ?: return
        val workspaceDir = File(context.filesDir, "workspaces/${activeProj.id}")

        if (!workspaceDir.exists()) {
            workspaceDir.mkdirs()
        }

        val files = workspaceDir.walkTopDown().maxDepth(3).filter { it.isFile }.toList()
        if (files.isEmpty()) {
            telegramApi.sendMessage(
                botToken = config.botToken,
                chatId = config.chatId,
                text = "📂 Workspace for *${activeProj.name}* is currently empty."
            )
            return
        }

        val text = buildString {
            appendLine("📂 *Workspace Files (${files.size} items):*")
            files.take(20).forEach { file ->
                val rel = file.relativeTo(workspaceDir).path
                appendLine("• `$rel` (${file.length()} bytes)")
            }
            if (files.size > 20) {
                appendLine("... and ${files.size - 20} more files.")
            }
        }

        telegramApi.sendMessage(
            botToken = config.botToken,
            chatId = config.chatId,
            text = text
        )
    }

    private suspend fun sendCatFileMessage(config: TelegramConfig, relativePath: String) {
        val projects = db.projectDao().getAllProjectsSync()
        val activeProj = projects.firstOrNull() ?: return
        val workspaceDir = File(context.filesDir, "workspaces/${activeProj.id}")
        val targetFile = File(workspaceDir, relativePath)

        if (!targetFile.exists() || !targetFile.isFile) {
            telegramApi.sendMessage(
                botToken = config.botToken,
                chatId = config.chatId,
                text = "❌ File not found: `$relativePath`"
            )
            return
        }

        val content = targetFile.readText().take(3000)
        val text = "📄 *File: `$relativePath`*\n```\n$content\n```"
        telegramApi.sendMessage(
            botToken = config.botToken,
            chatId = config.chatId,
            text = text
        )
    }

    private suspend fun executeDirectShellCommand(config: TelegramConfig, command: String) {
        val projects = db.projectDao().getAllProjectsSync()
        val activeProj = projects.firstOrNull() ?: return
        val workspaceDir = File(context.filesDir, "workspaces/${activeProj.id}").apply { mkdirs() }

        telegramApi.sendMessage(
            botToken = config.botToken,
            chatId = config.chatId,
            text = "⏳ Executing: `$command`..."
        )

        val taskId = UUID.randomUUID().toString()
        val res = shellEngine.executeCommand(
            taskId = taskId,
            command = command,
            workingDir = workspaceDir,
            approvedByUser = true
        )

        val output = if (res.stdout.isNotBlank()) res.stdout else res.stderr
        val text = """
            ⚡ *Shell Execution Finished*
            Command: `$command`
            Exit Code: `${res.exitCode}`
            
            ```
            ${output.take(3000)}
            ```
        """.trimIndent()

        telegramApi.sendMessage(
            botToken = config.botToken,
            chatId = config.chatId,
            text = text
        )
    }

    private suspend fun handleApproveCommand(config: TelegramConfig) {
        val pendingTask = db.taskDao().getLatestTaskSync()
        if (pendingTask == null || pendingTask.status != "AWAITING_APPROVAL" || pendingTask.requiresApprovalCommand == null) {
            telegramApi.sendMessage(
                botToken = config.botToken,
                chatId = config.chatId,
                text = "ℹ️ No sensitive command is currently awaiting approval."
            )
            return
        }

        val cmd = pendingTask.requiresApprovalCommand!!
        val workspaceDir = File(context.filesDir, "workspaces/${pendingTask.projectId}").apply { mkdirs() }

        telegramApi.sendMessage(
            botToken = config.botToken,
            chatId = config.chatId,
            text = "✅ Command approved via Telegram: `$cmd`\nExecuting now..."
        )

        val res = shellEngine.executeCommand(
            taskId = UUID.randomUUID().toString(),
            command = cmd,
            workingDir = workspaceDir,
            approvedByUser = true
        )

        db.chatDao().insertMessage(
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                sessionId = pendingTask.sessionId,
                role = "assistant",
                content = "User approved via Telegram: `$cmd`\n\nResult:\n```\n${res.stdout}\n${res.stderr}\n```\nExit code: ${res.exitCode}"
            )
        )

        telegramApi.sendMessage(
            botToken = config.botToken,
            chatId = config.chatId,
            text = "Execution completed:\n```\n${res.stdout.take(1500)}\n```"
        )
    }

    private suspend fun executeAgentPrompt(config: TelegramConfig, prompt: String) {
        val projects = db.projectDao().getAllProjectsSync()
        val project = projects.firstOrNull() ?: return
        val session = db.chatDao().getLatestSessionForProject(project.id)
            ?: db.chatDao().getSessionsForProjectSync(project.id).firstOrNull()
            ?: return

        // Insert Telegram prompt as user message
        db.chatDao().insertMessage(
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                sessionId = session.id,
                role = "user",
                content = "[Telegram] $prompt"
            )
        )

        telegramApi.sendMessage(
            botToken = config.botToken,
            chatId = config.chatId,
            text = "🚀 *Hermes Dev Autonomous Pipeline Started*\nTask: \"$prompt\"\nRunning 12-step verification..."
        )

        val taskRun = agentEngine.runAutonomousTask(
            sessionId = session.id,
            projectId = project.id,
            userPrompt = prompt,
            onStepUpdated = { step ->
                // Step progression
            },
            onLogMessage = { log ->
                // Log stream
            }
        )

        val finalMsg = when (taskRun.status) {
            VerificationStatus.COMPLETED_AND_VERIFIED.name -> {
                """
                    🎉 *Task Completed & Verified!*
                    Goal: $prompt
                    
                    ✅ *Evidence:*
                    `${taskRun.verificationEvidence ?: "Verified domain artifact generated"}`
                    
                    Check the Hermes app or use `/workspace` to view generated files.
                """.trimIndent()
            }
            "AWAITING_APPROVAL" -> {
                """
                    ⚠️ *Approval Required!*
                    Command: `${taskRun.requiresApprovalCommand}`
                    Reason: ${taskRun.requiresApprovalReason}
                    
                    Reply with `/approve` to execute this command.
                """.trimIndent()
            }
            else -> {
                """
                    ⚠️ *Hermes Dev Execution Alert*
                    Status: `${taskRun.status}`
                    Error: ${taskRun.errorDetails ?: "Task halted"}
                """.trimIndent()
            }
        }

        telegramApi.sendMessage(
            botToken = config.botToken,
            chatId = config.chatId,
            text = finalMsg
        )
    }
}
