package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val domain: String,
    val workspacePath: String,
    val gitBranch: String = "main",
    val gitStatus: String = "Clean",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val title: String,
    val activeProviderId: String = "gemini_default",
    val activeModel: String = "gemini-2.5-flash",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String, // "user", "assistant", "system", "tool"
    val content: String,
    val toolCallsJson: String? = null,
    val toolResultsJson: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String? = null,
    val taskId: String? = null
)

@Entity(tableName = "task_runs")
data class TaskRunEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val projectId: String,
    val userGoal: String,
    val currentStepName: String,
    val currentStepOrder: Int,
    val totalSteps: Int = 12,
    val status: String, // from VerificationStatus or "RUNNING", "PAUSED", "CANCELLED"
    val planJson: String = "[]",
    val errorDetails: String? = null,
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val verificationEvidence: String? = null,
    val requiresApprovalCommand: String? = null,
    val requiresApprovalReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null
)

@Entity(tableName = "execution_logs")
data class ExecutionLogEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val command: String,
    val workingDir: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val durationMs: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val requiresApproval: Boolean = false,
    val isApproved: Boolean = true
)

@Entity(tableName = "api_providers")
data class ApiProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val providerType: String, // GEMINI, OPENAI, ANTHROPIC, OLLAMA, CUSTOM_REST
    val baseUrl: String,
    val apiKey: String,
    val selectedModel: String,
    val priority: Int,
    val isEnabled: Boolean = true,
    val requestCount: Int = 0,
    val lastStatus: String = "Ready",
    val lastError: String? = null,
    val creditQuotaInfo: String = "Normal"
)

@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey val id: String,
    val name: String,
    val displayName: String,
    val description: String,
    val sourceUrl: String,
    val sourcePath: String,
    val version: String,
    val author: String,
    val isEnabled: Boolean = true,
    val permissionsJson: String = "[]",
    val dependenciesJson: String = "[]",
    val supportedPlatformsJson: String = "[\"Android\",\"Linux\",\"Docker\"]",
    val skillDocMarkdown: String,
    val installedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tool_dependencies")
data class ToolDependencyEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String, // SDK, COMPILER, RUNTIME, PACKAGE_MANAGER, CLI
    val commandCheck: String,
    val detectedVersion: String?,
    val isInstalled: Boolean,
    val status: String, // VERIFIED, MISSING, BROKEN, REQUIRES_PERMISSION
    val installCommand: String,
    val description: String
)

@Entity(tableName = "artifacts")
data class ArtifactEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val taskId: String?,
    val name: String,
    val relativePath: String,
    val fileSizeBytes: Long,
    val fileType: String, // APK, ZIP, SOURCE, BUILD_LOG, TEST_REPORT
    val checksum: String,
    val isVerified: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
