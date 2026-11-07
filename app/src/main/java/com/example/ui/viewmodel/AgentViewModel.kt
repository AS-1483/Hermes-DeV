package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ApiProviderEntity
import com.example.data.local.entity.ArtifactEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ChatSessionEntity
import com.example.data.local.entity.ExecutionLogEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.SkillEntity
import com.example.data.local.entity.TaskRunEntity
import com.example.data.local.entity.ToolDependencyEntity
import com.example.data.repository.ApiProviderRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.EnvironmentToolRepository
import com.example.data.repository.ProjectRepository
import com.example.data.repository.SkillRepository
import com.example.domain.engine.AiClientManager
import com.example.domain.engine.HermesAgentEngine
import com.example.domain.engine.ShellExecutionEngine
import com.example.domain.model.DevelopmentDomain
import com.example.domain.model.TaskStepState
import com.example.domain.model.VerificationStatus
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.data.telegram.TelegramApiService
import com.example.data.telegram.TelegramBotInfo
import com.example.data.telegram.TelegramConfig
import com.example.data.telegram.TelegramPreferencesRepository
import com.example.service.TelegramAgentService
import java.io.File
import java.util.UUID

class AgentViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val projectRepo = ProjectRepository(application, db.projectDao())
    private val chatRepo = ChatRepository(db.chatDao())
    private val skillRepo = SkillRepository(db.skillDao())
    private val toolRepo = EnvironmentToolRepository(db.toolDao())
    private val providerRepo = ApiProviderRepository(db.apiProviderDao())
    val telegramRepo = TelegramPreferencesRepository(application)
    val telegramApi = TelegramApiService()

    val telegramConfig: StateFlow<TelegramConfig> = telegramRepo.configFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TelegramConfig())

    private val shellEngine = ShellExecutionEngine(application, db.taskDao())
    private val aiClientManager = AiClientManager(db.apiProviderDao())
    private val agentEngine = HermesAgentEngine(
        context = application,
        taskDao = db.taskDao(),
        chatDao = db.chatDao(),
        artifactDao = db.artifactDao(),
        skillDao = db.skillDao(),
        aiClientManager = aiClientManager,
        shellExecutionEngine = shellEngine
    )

    // Projects
    val projects: StateFlow<List<ProjectEntity>> = projectRepo.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeProject = MutableStateFlow<ProjectEntity?>(null)
    val activeProject: StateFlow<ProjectEntity?> = _activeProject.asStateFlow()

    // Chat
    private val _sessions = MutableStateFlow<List<ChatSessionEntity>>(emptyList())
    val sessions: StateFlow<List<ChatSessionEntity>> = _sessions.asStateFlow()

    private val _currentSession = MutableStateFlow<ChatSessionEntity?>(null)
    val currentSession: StateFlow<ChatSessionEntity?> = _currentSession.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val messages: StateFlow<List<ChatMessageEntity>> = _messages.asStateFlow()

    // Autonomous task state
    private val _currentStep = MutableStateFlow(TaskStepState.UNDERSTANDING)
    val currentStep: StateFlow<TaskStepState> = _currentStep.asStateFlow()

    private val _isAgentBusy = MutableStateFlow(false)
    val isAgentBusy: StateFlow<Boolean> = _isAgentBusy.asStateFlow()

    private val _executionLiveLog = MutableStateFlow("")
    val executionLiveLog: StateFlow<String> = _executionLiveLog.asStateFlow()

    private val _pendingApproval = MutableStateFlow<Pair<String, String>?>(null) // command to reason
    val pendingApproval: StateFlow<Pair<String, String>?> = _pendingApproval.asStateFlow()

    // Skills
    val skills: StateFlow<List<SkillEntity>> = skillRepo.allSkills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tools
    val tools: StateFlow<List<ToolDependencyEntity>> = toolRepo.allTools
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // API Providers
    val providers: StateFlow<List<ApiProviderEntity>> = providerRepo.allProviders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Workspace Files
    private val _workspaceFiles = MutableStateFlow<List<File>>(emptyList())
    val workspaceFiles: StateFlow<List<File>> = _workspaceFiles.asStateFlow()

    private val _selectedFile = MutableStateFlow<File?>(null)
    val selectedFile: StateFlow<File?> = _selectedFile.asStateFlow()

    private val _fileEditorContent = MutableStateFlow("")
    val fileEditorContent: StateFlow<String> = _fileEditorContent.asStateFlow()

    // Logs
    val recentExecutionLogs: StateFlow<List<ExecutionLogEntity>> = db.taskDao().getAllRecentLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Artifacts
    val allArtifacts: StateFlow<List<ArtifactEntity>> = db.artifactDao().getAllArtifacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var messageCollectionJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Seed defaults
                skillRepo.populateDefaultHermesSkills()
                toolRepo.populateSystemTools()
                providerRepo.populateDefaultProviders()

                // Auto-create or pick default project
                val existing = db.projectDao().getAllProjectsSync()
                val targetProject = if (existing.isNotEmpty()) {
                    existing.first()
                } else {
                    projectRepo.createProject(
                        name = "Hermes Playground",
                        description = "Workspace for autonomous development, code generation, and shell testing.",
                        domain = DevelopmentDomain.ANDROID
                    )
                }
                withContext(Dispatchers.Main) {
                    selectProject(targetProject)
                }
            } catch (e: Exception) {
                android.util.Log.e("AgentViewModel", "Failed to initialize AgentViewModel", e)
            }
        }
    }

    private var sessionsCollectionJob: Job? = null

    fun selectProject(project: ProjectEntity) {
        _activeProject.value = project
        viewModelScope.launch {
            refreshWorkspaceFiles(project.id)

            // Collect all sessions for this project
            sessionsCollectionJob?.cancel()
            sessionsCollectionJob = viewModelScope.launch {
                chatRepo.getSessionsForProject(project.id).collect { list ->
                    _sessions.value = list
                }
            }

            // Load or reuse session
            val session = chatRepo.getOrCreateSession(project.id, "Session - ${project.name}")
            selectSession(session)
        }
    }

    fun selectSession(session: ChatSessionEntity) {
        _currentSession.value = session
        messageCollectionJob?.cancel()
        messageCollectionJob = viewModelScope.launch {
            chatRepo.getMessagesForSession(session.id).collect { msgs ->
                _messages.value = msgs
            }
        }
    }

    fun createNewSession(title: String = "New Development Session") {
        val project = _activeProject.value ?: return
        viewModelScope.launch {
            val session = chatRepo.createSession(project.id, title)
            selectSession(session)
        }
    }

    fun deleteSession(sessionId: String) {
        val project = _activeProject.value ?: return
        viewModelScope.launch {
            chatRepo.deleteSession(sessionId)
            val remaining = _sessions.value.filterNot { it.id == sessionId }
            if (_currentSession.value?.id == sessionId) {
                if (remaining.isNotEmpty()) {
                    selectSession(remaining.first())
                } else {
                    val newSession = chatRepo.createSession(project.id, "Session - ${project.name}")
                    selectSession(newSession)
                }
            }
        }
    }

    fun createProject(name: String, description: String, domain: DevelopmentDomain) {
        viewModelScope.launch {
            val project = projectRepo.createProject(name, description, domain)
            selectProject(project)
        }
    }

    fun refreshWorkspaceFiles(projectId: String) {
        viewModelScope.launch {
            val files = projectRepo.getWorkspaceFiles(projectId)
            _workspaceFiles.value = files
            if (files.isNotEmpty() && _selectedFile.value == null) {
                selectFile(files.first())
            }
        }
    }

    fun selectFile(file: File) {
        _selectedFile.value = file
        viewModelScope.launch {
            _fileEditorContent.value = projectRepo.readFileContent(file)
        }
    }

    fun updateEditorContent(newContent: String) {
        _fileEditorContent.value = newContent
    }

    fun saveCurrentFile() {
        val file = _selectedFile.value ?: return
        viewModelScope.launch {
            projectRepo.writeFileContent(file, _fileEditorContent.value)
            _activeProject.value?.let { refreshWorkspaceFiles(it.id) }
        }
    }

    fun sendMessage(userText: String) {
        val session = _currentSession.value ?: return
        val project = _activeProject.value ?: return
        if (userText.isBlank()) return

        viewModelScope.launch {
            val userMsg = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                sessionId = session.id,
                role = "user",
                content = userText
            )
            chatRepo.insertMessage(userMsg)

            _isAgentBusy.value = true
            _executionLiveLog.value = "[START] Initiating Hermes Autonomous Pipeline..."

            val taskRun = agentEngine.runAutonomousTask(
                sessionId = session.id,
                projectId = project.id,
                userPrompt = userText,
                onStepUpdated = { step ->
                    _currentStep.value = step
                },
                onLogMessage = { log ->
                    _executionLiveLog.value = log
                }
            )

            if (taskRun.status == "AWAITING_APPROVAL" && taskRun.requiresApprovalCommand != null) {
                _pendingApproval.value = Pair(taskRun.requiresApprovalCommand, taskRun.requiresApprovalReason ?: "Safety check")
                NotificationHelper.sendApprovalNotification(getApplication(), taskRun.requiresApprovalCommand)
                val tgCfg = telegramConfig.value
                if (tgCfg.isEnabled && tgCfg.notifyOnApprovalNeeded && tgCfg.botToken.isNotBlank() && tgCfg.chatId.isNotBlank()) {
                    telegramApi.sendMessage(
                        botToken = tgCfg.botToken,
                        chatId = tgCfg.chatId,
                        text = "⚠️ *Approval Required in Hermes Dev*\nCommand: `${taskRun.requiresApprovalCommand}`\nReason: ${taskRun.requiresApprovalReason}\nReply `/approve` to permit execution."
                    )
                }
            } else if (taskRun.status == VerificationStatus.COMPLETED_AND_VERIFIED.name) {
                NotificationHelper.sendTaskNotification(
                    getApplication(),
                    "Hermes Task Delivered",
                    taskRun.verificationEvidence ?: "Completed task: $userText"
                )
                val tgCfg = telegramConfig.value
                if (tgCfg.isEnabled && tgCfg.notifyOnTaskComplete && tgCfg.botToken.isNotBlank() && tgCfg.chatId.isNotBlank()) {
                    telegramApi.sendMessage(
                        botToken = tgCfg.botToken,
                        chatId = tgCfg.chatId,
                        text = "🎉 *Hermes Task Completed & Verified!*\nGoal: \"$userText\"\n\nEvidence: ${taskRun.verificationEvidence ?: "Artifacts verified."}"
                    )
                }
            } else if (taskRun.status == VerificationStatus.FAILED.name) {
                NotificationHelper.sendTaskNotification(
                    getApplication(),
                    "Hermes Task Alert",
                    taskRun.errorDetails ?: "Task execution halted."
                )
            }

            _isAgentBusy.value = false
            refreshWorkspaceFiles(project.id)
        }
    }

    fun approvePendingCommand() {
        val pending = _pendingApproval.value ?: return
        _pendingApproval.value = null
        val project = _activeProject.value ?: return
        val session = _currentSession.value ?: return

        viewModelScope.launch {
            _isAgentBusy.value = true
            val workspaceDir = File(getApplication<Application>().filesDir, "workspaces/${project.id}")
            val res = shellEngine.executeCommand(
                taskId = UUID.randomUUID().toString(),
                command = pending.first,
                workingDir = workspaceDir,
                approvedByUser = true
            )
            chatRepo.insertMessage(
                ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = session.id,
                    role = "assistant",
                    content = "User approved command: `${pending.first}`\n\nResult:\n```\n${res.stdout}\n${res.stderr}\n```\nExit code: ${res.exitCode}"
                )
            )
            _isAgentBusy.value = false
            refreshWorkspaceFiles(project.id)
        }
    }

    fun rejectPendingCommand() {
        _pendingApproval.value = null
    }

    fun toggleSkill(id: String, isEnabled: Boolean) {
        viewModelScope.launch {
            skillRepo.toggleSkill(id, isEnabled)
        }
    }

    fun importSkillFromGitHub(repoUrl: String, subfolder: String = "") {
        viewModelScope.launch {
            skillRepo.importFromGitHub(repoUrl, "main", subfolder)
        }
    }

    fun saveProvider(provider: ApiProviderEntity) {
        viewModelScope.launch {
            providerRepo.saveProvider(provider)
        }
    }

    fun deleteProvider(id: String) {
        viewModelScope.launch {
            providerRepo.deleteProvider(id)
        }
    }

    fun runToolDiagnostic(tool: ToolDependencyEntity) {
        val project = _activeProject.value ?: return
        viewModelScope.launch {
            val workspaceDir = File(getApplication<Application>().filesDir, "workspaces/${project.id}")
            val result = shellEngine.executeCommand(
                taskId = UUID.randomUUID().toString(),
                command = tool.commandCheck,
                workingDir = workspaceDir
            )
            val updated = tool.copy(
                isInstalled = true,
                status = if (result.exitCode == 0) "VERIFIED" else "CHECKED",
                detectedVersion = result.stdout.ifBlank { tool.detectedVersion ?: "Active" }
            )
            db.toolDao().insertOrUpdate(updated)
        }
    }

    // Telegram Bot Control
    fun saveTelegramConfig(
        botToken: String,
        chatId: String,
        isEnabled: Boolean,
        notifyOnTaskComplete: Boolean = true,
        notifyOnApprovalNeeded: Boolean = true,
        allowRemoteExecution: Boolean = true
    ) {
        viewModelScope.launch {
            telegramRepo.saveConfig(
                botToken = botToken,
                chatId = chatId,
                isEnabled = isEnabled,
                notifyOnTaskComplete = notifyOnTaskComplete,
                notifyOnApprovalNeeded = notifyOnApprovalNeeded,
                allowRemoteExecution = allowRemoteExecution
            )
            if (isEnabled && botToken.isNotBlank() && chatId.isNotBlank()) {
                TelegramAgentService.startService(getApplication())
            } else {
                TelegramAgentService.stopService(getApplication())
            }
        }
    }

    fun toggleTelegramEnabled(enabled: Boolean) {
        viewModelScope.launch {
            telegramRepo.toggleEnabled(enabled)
            val cfg = telegramConfig.value
            if (enabled && cfg.botToken.isNotBlank() && cfg.chatId.isNotBlank()) {
                TelegramAgentService.startService(getApplication())
            } else {
                TelegramAgentService.stopService(getApplication())
            }
        }
    }

    suspend fun verifyTelegramBot(botToken: String): Result<TelegramBotInfo> {
        return telegramApi.getMe(botToken)
    }

    suspend fun sendTelegramTestMessage(botToken: String, chatId: String): Result<Boolean> {
        return telegramApi.sendMessage(
            botToken = botToken,
            chatId = chatId,
            text = "⚡ *Hermes Dev Agent Connected!*\n\nTelegram configuration verified successfully. You have full remote control over the agent. Send `/help` for commands."
        )
    }
}
