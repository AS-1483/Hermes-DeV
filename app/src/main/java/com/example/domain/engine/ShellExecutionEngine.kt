package com.example.domain.engine

import android.content.Context
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.ExecutionLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.UUID
import java.util.concurrent.TimeUnit

data class CommandExecutionResult(
    val command: String,
    val workingDir: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val durationMs: Long,
    val isVerified: Boolean,
    val requiresApproval: Boolean = false,
    val approvalGranted: Boolean = true
)

class ShellExecutionEngine(
    private val context: Context,
    private val taskDao: TaskDao
) {
    // Dangerous command patterns requiring explicit user approval
    private val dangerousPatterns = listOf(
        "rm -rf /",
        "rm -rf *",
        "dd if=",
        "mkfs",
        ":(){ :|:& };:",
        "chmod 777",
        "> /dev/sd",
        "curl http",
        "wget http"
    )

    fun requiresApproval(command: String): Boolean {
        val trimmed = command.trim().lowercase()
        return dangerousPatterns.any { trimmed.contains(it) } ||
               trimmed.startsWith("rm -rf") ||
               trimmed.contains("sudo")
    }

    suspend fun executeCommand(
        taskId: String,
        command: String,
        workingDir: File,
        envVars: Map<String, String> = emptyMap(),
        timeoutSeconds: Long = 60,
        approvedByUser: Boolean = false
    ): CommandExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        if (requiresApproval(command) && !approvedByUser) {
            val log = ExecutionLogEntity(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                command = command,
                workingDir = workingDir.absolutePath,
                stdout = "[SAFETY GUARD] Command paused. Requires explicit user authorization.",
                stderr = "Awaiting user approval for sensitive operation.",
                exitCode = -1,
                durationMs = 0,
                requiresApproval = true,
                isApproved = false
            )
            taskDao.insertLog(log)
            return@withContext CommandExecutionResult(
                command = command,
                workingDir = workingDir.absolutePath,
                stdout = log.stdout,
                stderr = log.stderr,
                exitCode = -1,
                durationMs = 0,
                isVerified = false,
                requiresApproval = true,
                approvalGranted = false
            )
        }

        if (!workingDir.exists()) {
            workingDir.mkdirs()
        }

        var process: Process? = null
        val stdoutBuilder = StringBuilder()
        val stderrBuilder = StringBuilder()
        var exitCode = -1

        try {
            // Android ProcessBuilder execution in app workspace sandbox
            val processBuilder = ProcessBuilder("sh", "-c", command)
            processBuilder.directory(workingDir)
            val currentEnv = processBuilder.environment()
            currentEnv.putAll(envVars)
            currentEnv["WORKSPACE"] = workingDir.absolutePath
            currentEnv["HOME"] = context.filesDir.absolutePath
            currentEnv["TMPDIR"] = context.cacheDir.absolutePath

            process = processBuilder.start()

            val readerOut = BufferedReader(InputStreamReader(process.inputStream))
            val readerErr = BufferedReader(InputStreamReader(process.errorStream))

            val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (completed) {
                readerOut.forEachLine { line -> stdoutBuilder.appendLine(line) }
                readerErr.forEachLine { line -> stderrBuilder.appendLine(line) }
                exitCode = process.exitValue()
            } else {
                process.destroyForcibly()
                stderrBuilder.appendLine("[TIMEOUT] Command exceeded ${timeoutSeconds}s limit and was terminated.")
                exitCode = 124
            }
        } catch (e: Exception) {
            stderrBuilder.appendLine("[EXECUTION ERROR] ${e.localizedMessage}")
            exitCode = 1
        } finally {
            process?.destroy()
        }

        val duration = System.currentTimeMillis() - startTime
        val stdoutStr = stdoutBuilder.toString().trim()
        val stderrStr = stderrBuilder.toString().trim()
        val isVerified = exitCode == 0

        val log = ExecutionLogEntity(
            id = UUID.randomUUID().toString(),
            taskId = taskId,
            command = command,
            workingDir = workingDir.absolutePath,
            stdout = stdoutStr.take(15000), // bounded log capture
            stderr = stderrStr.take(15000),
            exitCode = exitCode,
            durationMs = duration,
            requiresApproval = requiresApproval(command),
            isApproved = true
        )
        taskDao.insertLog(log)

        CommandExecutionResult(
            command = command,
            workingDir = workingDir.absolutePath,
            stdout = stdoutStr,
            stderr = stderrStr,
            exitCode = exitCode,
            durationMs = duration,
            isVerified = isVerified,
            requiresApproval = false,
            approvalGranted = true
        )
    }

    /**
     * Anti-Fake-Work verification: verifies that an expected file or build artifact actually exists,
     * is non-empty, and returns its true byte size.
     */
    fun verifyFileArtifact(workingDir: File, relativePath: String): Pair<Boolean, Long> {
        val target = File(workingDir, relativePath)
        return if (target.exists() && target.length() > 0) {
            Pair(true, target.length())
        } else {
            Pair(false, 0L)
        }
    }
}
