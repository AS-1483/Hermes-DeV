package com.example.ui.screens.skills

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.local.entity.SkillEntity
import com.example.ui.components.StatusPill
import com.example.ui.theme.*
import com.example.ui.viewmodel.AgentViewModel

@Composable
fun SkillStoreScreen(
    viewModel: AgentViewModel,
    modifier: Modifier = Modifier
) {
    val skills by viewModel.skills.collectAsState()

    var showImportDialog by remember { mutableStateOf(false) }
    var selectedSkillForDoc by remember { mutableStateOf<SkillEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkBg)
            .padding(14.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "GitHub Skill Manager",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Hermes SKILL.md specs & tool-calling discovery",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Button(
                onClick = { showImportDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("import_skill_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = TerminalBlack,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Import Skill", color = TerminalBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Skills List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(skills, key = { it.id }) { skill ->
                SkillCard(
                    skill = skill,
                    onToggle = { viewModel.toggleSkill(skill.id, it) },
                    onViewDoc = { selectedSkillForDoc = skill }
                )
            }
        }
    }

    // Import from GitHub Dialog
    if (showImportDialog) {
        ImportSkillDialog(
            onDismiss = { showImportDialog = false },
            onImport = { url, folder ->
                viewModel.importSkillFromGitHub(url, folder)
                showImportDialog = false
            }
        )
    }

    // Skill Documentation Viewer Modal
    if (selectedSkillForDoc != null) {
        SkillDocModal(
            skill = selectedSkillForDoc!!,
            onDismiss = { selectedSkillForDoc = null }
        )
    }
}

@Composable
fun SkillCard(
    skill: SkillEntity,
    onToggle: (Boolean) -> Unit,
    onViewDoc: () -> Unit
) {
    Surface(
        color = CyberCard,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (skill.isEnabled) CyanPrimary.copy(alpha = 0.4f) else CyberCardStroke
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (skill.isEnabled) CyanPrimary.copy(alpha = 0.15f) else Color(0xFF334155)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = if (skill.isEnabled) CyanPrimary else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = skill.displayName,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "v${skill.version} by ${skill.author}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                Switch(
                    checked = skill.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyanPrimary,
                        checkedTrackColor = CyanPrimary.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberDarkBg
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = skill.description,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = skill.sourceUrl.removePrefix("https://github.com/"),
                    color = AmberAccent,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                TextButton(
                    onClick = onViewDoc,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Read SKILL.md",
                        tint = CyanPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SKILL.md", color = CyanPrimary, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun ImportSkillDialog(
    onDismiss: () -> Unit,
    onImport: (String, String) -> Unit
) {
    var repoUrl by remember { mutableStateOf("https://github.com/NousResearch/hermes-agent.git") }
    var subfolder by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudDownload, null, tint = CyanPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import Skill from GitHub", color = TextPrimary)
            }
        },
        text = {
            Column {
                Text(
                    text = "Enter a public GitHub repository or official skill package URL:",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = repoUrl,
                    onValueChange = { repoUrl = it },
                    label = { Text("Repository URL") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // Quick buttons for official repos
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    OutlinedButton(
                        onClick = { repoUrl = "https://github.com/NousResearch/hermes-agent.git" },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("Nous Hermes", fontSize = 10.sp, color = CyanPrimary)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = { repoUrl = "https://github.com/morluto/rea.git" },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("morluto/rea", fontSize = 10.sp, color = AmberAccent)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = subfolder,
                    onValueChange = { subfolder = it },
                    label = { Text("Subfolder (optional)") },
                    placeholder = { Text("e.g. skills/android") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(repoUrl, subfolder) },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("Validate & Install", color = TerminalBlack, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun SkillDocModal(
    skill: SkillEntity,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Description, null, tint = CyanPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(skill.displayName, color = TextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                Surface(
                    color = TerminalBlack,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardStroke),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = skill.skillDocMarkdown,
                        color = TerminalGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("Close", color = TerminalBlack)
            }
        }
    )
}
