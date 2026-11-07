package com.example.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ChatMessageEntity
import com.example.domain.model.DevelopmentDomain
import com.example.ui.components.CodeBlockView
import com.example.ui.components.CommandApprovalDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.TaskTimelineView
import com.example.ui.theme.*
import com.example.ui.viewmodel.AgentViewModel

@Composable
fun AgentChatScreen(
    viewModel: AgentViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val isBusy by viewModel.isAgentBusy.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    val liveLog by viewModel.executionLiveLog.collectAsState()
    val pendingApproval by viewModel.pendingApproval.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Pending dangerous command approval dialog
    if (pendingApproval != null) {
        val (cmd, reason) = pendingApproval!!
        CommandApprovalDialog(
            command = cmd,
            reason = reason,
            onApprove = { viewModel.approvePendingCommand() },
            onReject = { viewModel.rejectPendingCommand() }
        )
    }

    val currentSession by viewModel.currentSession.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkBg)
    ) {
        // Session Header Bar
        Surface(
            color = CyberSurface,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardStroke),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = null,
                        tint = AmberAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentSession?.title ?: "Development Session",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                TextButton(
                    onClick = {
                        viewModel.createNewSession("Session #${(System.currentTimeMillis() / 1000) % 10000}")
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Session",
                        tint = CyanPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("+ New Session", color = CyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Timeline Header when executing or active
        if (isBusy || messages.isNotEmpty()) {
            TaskTimelineView(
                currentStepOrder = currentStep.order,
                currentStepName = currentStep.label,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )
        }

        // Live Log Banner
        AnimatedVisibility(visible = isBusy && liveLog.isNotBlank()) {
            Surface(
                color = TerminalBlack,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = CyanPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = liveLog,
                        color = TerminalGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }
        }

        // Messages List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty() && !isBusy) {
                ChatEmptyState(
                    onSelectPreset = { prompt ->
                        inputText = prompt
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageItem(message = msg)
                    }
                }
            }
        }

        // Quick Domain Suggestion Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            DevelopmentDomain.values().forEach { domain ->
                Surface(
                    color = CyberCard,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardStroke),
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .clickable {
                            inputText = domain.defaultPrompt
                        }
                ) {
                    Text(
                        text = domain.title,
                        color = CyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Chat Input Bar
        Surface(
            color = CyberSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardStroke),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                IconButton(
                    onClick = {
                        inputText = "Inspect project workspace, list file tree, and verify dependencies."
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("inspect_workspace_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Inspect Workspace",
                        tint = AmberAccent
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "Ask Hermes Dev to build, code, run, or debug...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CyberDarkBg,
                        unfocusedContainerColor = CyberDarkBg,
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = CyberCardStroke,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(20.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                FloatingActionButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isBusy) {
                            val text = inputText
                            inputText = ""
                            viewModel.sendMessage(text)
                        }
                    },
                    containerColor = if (inputText.isNotBlank() && !isBusy) CyanPrimary else CyanPrimary.copy(alpha = 0.3f),
                    contentColor = TerminalBlack,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("send_message_button"),
                    shape = CircleShape
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = TerminalBlack,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Message",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessageEntity) {
    val isUser = message.role == "user"
    val clipboard: ClipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Role & Info Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp, end = 4.dp)
        ) {
            Icon(
                imageVector = if (isUser) Icons.Default.Person else Icons.Default.Terminal,
                contentDescription = null,
                tint = if (isUser) AmberAccent else CyanPrimary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isUser) "Developer" else "Hermes Dev Agent",
                color = if (isUser) AmberAccent else CyanPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            if (message.modelUsed != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "• ${message.modelUsed}",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }

        // Message Card
        Surface(
            color = if (isUser) Color(0xFF1E293B) else CyberCard,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isUser) CyanPrimary.copy(alpha = 0.3f) else CyberCardStroke
            ),
            modifier = Modifier.widthIn(max = 580.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Parse message text and code blocks
                val parsedBlocks = remember(message.content) {
                    parseMarkdownCodeBlocks(message.content)
                }

                parsedBlocks.forEach { block ->
                    when (block) {
                        is ContentBlock.Text -> {
                            Text(
                                text = block.text,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                        is ContentBlock.Code -> {
                            CodeBlockView(
                                code = block.code,
                                language = block.language
                            )
                        }
                    }
                }

                // Copy Message Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(message.content))
                            copied = true
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Icon(
                            imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy message",
                            tint = if (copied) EmeraldSuccess else TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (copied) "Copied" else "Copy",
                            color = if (copied) EmeraldSuccess else TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatEmptyState(onSelectPreset: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CyanPrimary.copy(alpha = 0.15f))
                .border(1.dp, CyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Hermes Dev Agent",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = "Autonomous AI Software Engineer inspired by Hermes Agent & Rea",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        val prompts = listOf(
            "Build an Android app with Jetpack Compose, Room database, and M3 design." to "Android App",
            "Develop a responsive Next.js / TypeScript dashboard with Tailwind CSS." to "Web App",
            "Create a FastAPI backend with REST endpoints, Pydantic schemas, and tests." to "FastAPI Backend",
            "Develop ESP32-S3 firmware with PlatformIO, Wi-Fi telemetry, and BLE." to "ESP32 Firmware"
        )

        prompts.forEach { (prompt, label) ->
            Surface(
                color = CyberCard,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardStroke),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(vertical = 4.dp)
                    .clickable { onSelectPreset(prompt) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Bolt, null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = label, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowForward, null, tint = CyanPrimary, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

sealed class ContentBlock {
    data class Text(val text: String) : ContentBlock()
    data class Code(val code: String, val language: String) : ContentBlock()
}

fun parseMarkdownCodeBlocks(content: String): List<ContentBlock> {
    val blocks = mutableListOf<ContentBlock>()
    val lines = content.lines()
    var inCodeBlock = false
    var currentLang = "code"
    val codeBuilder = StringBuilder()
    val textBuilder = StringBuilder()

    for (line in lines) {
        if (line.trim().startsWith("```")) {
            if (inCodeBlock) {
                // Closing code block
                blocks.add(ContentBlock.Code(codeBuilder.toString().trimEnd(), currentLang))
                codeBuilder.clear()
                inCodeBlock = false
            } else {
                // Opening code block
                if (textBuilder.isNotBlank()) {
                    blocks.add(ContentBlock.Text(textBuilder.toString().trimEnd()))
                    textBuilder.clear()
                }
                currentLang = line.trim().removePrefix("```").trim().ifBlank { "code" }
                inCodeBlock = true
            }
        } else {
            if (inCodeBlock) {
                codeBuilder.appendLine(line)
            } else {
                textBuilder.appendLine(line)
            }
        }
    }

    if (inCodeBlock && codeBuilder.isNotBlank()) {
        blocks.add(ContentBlock.Code(codeBuilder.toString().trimEnd(), currentLang))
    } else if (textBuilder.isNotBlank()) {
        blocks.add(ContentBlock.Text(textBuilder.toString().trimEnd()))
    }

    return blocks
}
