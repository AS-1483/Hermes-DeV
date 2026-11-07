package com.example.ui.screens.telegram

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.telegram.TelegramBotInfo
import com.example.ui.theme.*
import com.example.ui.viewmodel.AgentViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TelegramConfigScreen(
    viewModel: AgentViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.telegramConfig.collectAsState()
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    var botTokenInput by remember(config.botToken) { mutableStateOf(config.botToken) }
    var chatIdInput by remember(config.chatId) { mutableStateOf(config.chatId) }
    var tokenVisible by remember { mutableStateOf(false) }

    var isVerifying by remember { mutableStateOf(false) }
    var verificationResult by remember { mutableStateOf<TelegramBotInfo?>(null) }
    var verificationError by remember { mutableStateOf<String?>(null) }

    var isSendingTest by remember { mutableStateOf(false) }
    var testMessageStatus by remember { mutableStateOf<String?>(null) }

    var notifyTaskComplete by remember(config.notifyOnTaskComplete) { mutableStateOf(config.notifyOnTaskComplete) }
    var notifyApproval by remember(config.notifyOnApprovalNeeded) { mutableStateOf(config.notifyOnApprovalNeeded) }
    var allowRemoteExecution by remember(config.allowRemoteExecution) { mutableStateOf(config.allowRemoteExecution) }

    val isConnected = config.isEnabled && config.botToken.isNotBlank() && config.chatId.isNotBlank()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = CyberDarkBg
    ) { paddingVals ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingVals)
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyanPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Telegram Bot Remote",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Full interactive control via Telegram Bot API",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Global On/Off Switch
                Switch(
                    checked = config.isEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && (botTokenInput.isBlank() || chatIdInput.isBlank())) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Please enter Bot Token & Chat ID first")
                            }
                        } else {
                            viewModel.saveTelegramConfig(
                                botToken = botTokenInput,
                                chatId = chatIdInput,
                                isEnabled = enabled,
                                notifyOnTaskComplete = notifyTaskComplete,
                                notifyOnApprovalNeeded = notifyApproval,
                                allowRemoteExecution = allowRemoteExecution
                            )
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyanPrimary,
                        checkedTrackColor = CyanPrimary.copy(alpha = 0.35f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberDarkBg
                    ),
                    modifier = Modifier.testTag("telegram_global_toggle")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Real-time Status Card
            Surface(
                color = CyberCard,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isConnected) EmeraldSuccess.copy(alpha = 0.5f) else AmberAccent.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) EmeraldSuccess else AmberAccent)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isConnected) "Bot Controller Active & Polling" else "Bot Controller Idle / Disconnected",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (isConnected) {
                                "Authorized Chat ID: ${config.chatId} • Background polling running"
                            } else {
                                "Set token and chat ID, then turn ON switch to enable remote commands"
                            },
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    if (config.lastSyncTime > 0) {
                        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(config.lastSyncTime))
                        Text(
                            text = "Sync: $timeStr",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Credentials Card
            Surface(
                color = CyberSurface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardStroke),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Telegram Bot Credentials",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Create a bot on Telegram via @BotFather to get your token. Get your Chat ID via @userinfobot.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bot Token Field
                    OutlinedTextField(
                        value = botTokenInput,
                        onValueChange = {
                            botTokenInput = it
                            verificationResult = null
                            verificationError = null
                        },
                        label = { Text("Bot Token (e.g. 123456789:ABCdef...)") },
                        visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                Icon(
                                    imageVector = if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = TextSecondary
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("telegram_bot_token_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = CyberCardStroke,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Chat ID Field
                    OutlinedTextField(
                        value = chatIdInput,
                        onValueChange = {
                            chatIdInput = it
                            testMessageStatus = null
                        },
                        label = { Text("User / Group Chat ID (e.g. 987654321)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("telegram_chat_id_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = CyberCardStroke,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Verification Info / Errors
                    if (verificationResult != null) {
                        Surface(
                            color = EmeraldSuccess.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Bot Verified: @${verificationResult!!.username} (${verificationResult!!.firstName})",
                                    color = EmeraldSuccess,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (verificationError != null) {
                        Surface(
                            color = RoseError.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Icon(Icons.Default.Error, null, tint = RoseError, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = verificationError!!,
                                    color = RoseError,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    if (testMessageStatus != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = testMessageStatus!!,
                            color = if (testMessageStatus!!.startsWith("✅")) EmeraldSuccess else RoseError,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons (Verify Token, Test Message, Save)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (botTokenInput.isBlank()) {
                                    verificationError = "Enter bot token first"
                                    return@OutlinedButton
                                }
                                isVerifying = true
                                verificationError = null
                                verificationResult = null
                                scope.launch {
                                    val res = viewModel.verifyTelegramBot(botTokenInput)
                                    isVerifying = false
                                    res.onSuccess { info ->
                                        verificationResult = info
                                    }.onFailure { err ->
                                        verificationError = err.message ?: "Failed to verify token"
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isVerifying) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = CyanPrimary)
                            } else {
                                Icon(Icons.Default.Verified, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Verify Bot", fontSize = 11.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                if (botTokenInput.isBlank() || chatIdInput.isBlank()) {
                                    testMessageStatus = "❌ Enter both Token & Chat ID"
                                    return@OutlinedButton
                                }
                                isSendingTest = true
                                testMessageStatus = null
                                scope.launch {
                                    val res = viewModel.sendTelegramTestMessage(botTokenInput, chatIdInput)
                                    isSendingTest = false
                                    res.onSuccess {
                                        testMessageStatus = "✅ Test message delivered to Telegram chat!"
                                    }.onFailure { err ->
                                        testMessageStatus = "❌ Error: ${err.message ?: "Could not deliver message"}"
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberAccent),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isSendingTest) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = AmberAccent)
                            } else {
                                Icon(Icons.Default.Send, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Send Test", fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.saveTelegramConfig(
                                    botToken = botTokenInput,
                                    chatId = chatIdInput,
                                    isEnabled = true,
                                    notifyOnTaskComplete = notifyTaskComplete,
                                    notifyOnApprovalNeeded = notifyApproval,
                                    allowRemoteExecution = allowRemoteExecution
                                )
                                scope.launch {
                                    snackbarHostState.showSnackbar("Telegram configuration saved & activated!")
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_telegram_config_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Save, null, tint = TerminalBlack, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", color = TerminalBlack, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Automation & Permissions Options
            Surface(
                color = CyberSurface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardStroke),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Telegram Control Settings",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Allow Remote Execution (/sh & /run)", color = TextPrimary, fontSize = 13.sp)
                            Text("Let Telegram bot execute shell scripts and autonomous tasks", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = allowRemoteExecution,
                            onCheckedChange = {
                                allowRemoteExecution = it
                                viewModel.saveTelegramConfig(
                                    botToken = botTokenInput,
                                    chatId = chatIdInput,
                                    isEnabled = config.isEnabled,
                                    notifyOnTaskComplete = notifyTaskComplete,
                                    notifyOnApprovalNeeded = notifyApproval,
                                    allowRemoteExecution = it
                                )
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = CyberCardStroke)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Notify on Task Complete", color = TextPrimary, fontSize = 13.sp)
                            Text("Send notification with artifact evidence when task finishes", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = notifyTaskComplete,
                            onCheckedChange = {
                                notifyTaskComplete = it
                                viewModel.saveTelegramConfig(
                                    botToken = botTokenInput,
                                    chatId = chatIdInput,
                                    isEnabled = config.isEnabled,
                                    notifyOnTaskComplete = it,
                                    notifyOnApprovalNeeded = notifyApproval,
                                    allowRemoteExecution = allowRemoteExecution
                                )
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = CyberCardStroke)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Notify for Security Approvals", color = TextPrimary, fontSize = 13.sp)
                            Text("Send alert when a sensitive command requires `/approve`", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = notifyApproval,
                            onCheckedChange = {
                                notifyApproval = it
                                viewModel.saveTelegramConfig(
                                    botToken = botTokenInput,
                                    chatId = chatIdInput,
                                    isEnabled = config.isEnabled,
                                    notifyOnTaskComplete = notifyTaskComplete,
                                    notifyOnApprovalNeeded = it,
                                    allowRemoteExecution = allowRemoteExecution
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Telegram Commands Guide Card
            Surface(
                color = CyberCard,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardStroke),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Telegram Bot Commands",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        TextButton(
                            onClick = {
                                val commandsText = """
                                    /start - Welcome and setup check
                                    /help - Display command menu
                                    /status - Current project, step, and status
                                    /projects - List registered projects
                                    /sessions - List active sessions
                                    /workspace - List workspace files
                                    /cat <file> - Read workspace file
                                    /sh <command> - Execute shell command
                                    /run <prompt> - Run 12-step autonomous task
                                    /approve - Approve pending command
                                """.trimIndent()
                                clipboardManager.setText(AnnotatedString(commandsText))
                                scope.launch {
                                    snackbarHostState.showSnackbar("Commands copied to clipboard!")
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, null, tint = CyanPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy All", color = CyanPrimary, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val commands = listOf(
                        "Direct message" to "Sends prompt to Hermes autonomous agent",
                        "/run <prompt>" to "Execute autonomous 12-step engineering task",
                        "/sh <command>" to "Run shell command in workspace sandbox",
                        "/status" to "Show active project, task step & agent health",
                        "/projects" to "List all projects in database",
                        "/sessions" to "List chat sessions for current project",
                        "/workspace" to "List files in the active project directory",
                        "/cat <file>" to "Read workspace file content",
                        "/approve" to "Approve sensitive command waiting for confirmation",
                        "/help" to "Show all commands and usage guide"
                    )

                    commands.forEach { (cmd, desc) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Surface(
                                color = TerminalBlack,
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = cmd,
                                    color = CyanPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = desc,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
