package com.example.ui.screens.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ArtifactEntity
import com.example.data.local.entity.ExecutionLogEntity
import com.example.ui.components.StatusPill
import com.example.ui.theme.*
import com.example.ui.viewmodel.AgentViewModel
import java.io.File

@Composable
fun WorkspaceScreen(
    viewModel: AgentViewModel,
    modifier: Modifier = Modifier
) {
    val activeProject by viewModel.activeProject.collectAsState()
    val files by viewModel.workspaceFiles.collectAsState()
    val selectedFile by viewModel.selectedFile.collectAsState()
    val editorContent by viewModel.fileEditorContent.collectAsState()
    val logs by viewModel.recentExecutionLogs.collectAsState()
    val artifacts by viewModel.allArtifacts.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Editor, 1: Terminal Logs, 2: Artifacts
    val tabs = listOf("Code Editor", "Terminal Logs", "Verified Artifacts")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkBg)
    ) {
        // Workspace sub-tabs
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = CyberSurface,
            contentColor = CyanPrimary,
            divider = { HorizontalDivider(color = CyberCardStroke) }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = activeTab == index,
                    onClick = { activeTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (activeTab == index) CyanPrimary else TextSecondary,
                            fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        when (activeTab) {
            0 -> CodeEditorPane(
                files = files,
                selectedFile = selectedFile,
                editorContent = editorContent,
                onSelectFile = { viewModel.selectFile(it) },
                onContentChange = { viewModel.updateEditorContent(it) },
                onSave = { viewModel.saveCurrentFile() },
                onAskAgent = {
                    selectedFile?.let { f ->
                        viewModel.sendMessage("Review, refactor, and write tests for file: ${f.name}\n\n```\n$editorContent\n```")
                    }
                }
            )
            1 -> TerminalLogsPane(logs = logs)
            2 -> ArtifactsPane(artifacts = artifacts)
        }
    }
}

@Composable
fun CodeEditorPane(
    files: List<File>,
    selectedFile: File?,
    editorContent: String,
    onSelectFile: (File) -> Unit,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onAskAgent: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // File tabs bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            if (files.isEmpty()) {
                Text(
                    text = "No files created yet. Ask Hermes Dev to generate code!",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            } else {
                files.forEach { file ->
                    val isSelected = selectedFile?.absolutePath == file.absolutePath
                    Surface(
                        color = if (isSelected) CyberCard else Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)) else null,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clickable { onSelectFile(file) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    file.name.endsWith(".kt") -> Icons.Default.Android
                                    file.name.endsWith(".tsx") || file.name.endsWith(".js") -> Icons.Default.Web
                                    file.name.endsWith(".py") -> Icons.Default.Code
                                    file.name.endsWith(".cpp") -> Icons.Default.Memory
                                    else -> Icons.Default.InsertDriveFile
                                },
                                contentDescription = null,
                                tint = if (isSelected) CyanPrimary else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = file.name,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // Action Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberSurface)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = selectedFile?.name ?: "No file open",
                color = TextPrimary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
            Row {
                OutlinedButton(
                    onClick = onAskAgent,
                    enabled = selectedFile != null,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.AutoFixHigh, null, tint = CyanPrimary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ask Agent", fontSize = 11.sp, color = CyanPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onSave,
                    enabled = selectedFile != null,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.Save, null, tint = TerminalBlack, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save", fontSize = 11.sp, color = TerminalBlack, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Editor TextArea
        Surface(
            color = TerminalBlack,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            TextField(
                value = editorContent,
                onValueChange = onContentChange,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("code_editor_field"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = TerminalBlack,
                    unfocusedContainerColor = TerminalBlack,
                    focusedTextColor = TerminalGreen,
                    unfocusedTextColor = TerminalGreen,
                    cursorColor = CyanPrimary
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            )
        }
    }
}

@Composable
fun TerminalLogsPane(logs: List<ExecutionLogEntity>) {
    if (logs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No shell commands executed yet.",
                color = TextMuted,
                fontSize = 13.sp
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(logs, key = { it.id }) { log ->
                Surface(
                    color = TerminalBlack,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (log.exitCode == 0) CyberCardStroke else RoseError.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "$ ${log.command}",
                                color = CyanPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            StatusPill(
                                status = if (log.exitCode == 0) "Exit 0" else "Exit ${log.exitCode}"
                            )
                        }

                        if (log.stdout.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = log.stdout,
                                color = TerminalGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }

                        if (log.stderr.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = log.stderr,
                                color = RoseError,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Dir: ${log.workingDir}",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${log.durationMs}ms",
                                color = AmberAccent,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtifactsPane(artifacts: List<ArtifactEntity>) {
    if (artifacts.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No verified build artifacts generated yet.\nRun an autonomous build task to compile APKs, packages, or firmware.",
                color = TextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            items(artifacts, key = { it.id }) { art ->
                Surface(
                    color = CyberCard,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardStroke),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyanPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (art.fileType) {
                                    "APK" -> Icons.Default.Android
                                    "FIRMWARE" -> Icons.Default.Memory
                                    else -> Icons.Default.Code
                                },
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = art.name,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${art.fileType} • ${art.fileSizeBytes} bytes • ${art.checksum.take(16)}",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        StatusPill(status = if (art.isVerified) "VERIFIED" else "UNVERIFIED")
                    }
                }
            }
        }
    }
}
