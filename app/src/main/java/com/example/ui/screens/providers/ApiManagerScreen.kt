package com.example.ui.screens.providers

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ApiProviderEntity
import com.example.domain.model.ProviderType
import com.example.ui.components.StatusPill
import com.example.ui.theme.*
import com.example.ui.viewmodel.AgentViewModel
import java.util.UUID

@Composable
fun ApiManagerScreen(
    viewModel: AgentViewModel,
    modifier: Modifier = Modifier
) {
    val providers by viewModel.providers.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingProvider by remember { mutableStateOf<ApiProviderEntity?>(null) }

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
                    text = "API Manager & Failover",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Multi-provider routing: Gemini, OpenAI, Claude & Ollama",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("add_provider_button")
            ) {
                Icon(Icons.Default.Add, null, tint = TerminalBlack, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Key", color = TerminalBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Security Notice Banner
        Surface(
            color = CyberSurface,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardStroke),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(10.dp)
            ) {
                Icon(Icons.Default.Lock, null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Credentials encrypted locally in app sandbox. Keys are never logged or exposed.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Provider cards list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(providers, key = { it.id }) { provider ->
                ProviderCard(
                    provider = provider,
                    onToggleEnabled = {
                        viewModel.saveProvider(provider.copy(isEnabled = it))
                    },
                    onEdit = {
                        editingProvider = provider
                    },
                    onDelete = {
                        viewModel.deleteProvider(provider.id)
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        ProviderEditDialog(
            existing = null,
            onDismiss = { showAddDialog = false },
            onSave = {
                viewModel.saveProvider(it)
                showAddDialog = false
            }
        )
    }

    if (editingProvider != null) {
        ProviderEditDialog(
            existing = editingProvider,
            onDismiss = { editingProvider = null },
            onSave = {
                viewModel.saveProvider(it)
                editingProvider = null
            }
        )
    }
}

@Composable
fun ProviderCard(
    provider: ApiProviderEntity,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = CyberCard,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (provider.isEnabled) CyanPrimary.copy(alpha = 0.4f) else CyberCardStroke
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
                            .background(CyanPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = provider.name,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = AmberAccent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Priority ${provider.priority}",
                                    color = AmberAccent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "${provider.providerType} • ${provider.selectedModel}",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Switch(
                    checked = provider.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyanPrimary,
                        checkedTrackColor = CyanPrimary.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberDarkBg
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Details & Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Key: ${if (provider.apiKey.isNotBlank()) "••••••••" + provider.apiKey.takeLast(4) else "No Key Set (Local/Env)"}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Requests: ${provider.requestCount}",
                    color = CyanPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (provider.lastError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Last Error: ${provider.lastError}",
                    color = RoseError,
                    fontSize = 11.sp,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, null, tint = CyanPrimary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Configure", color = CyanPrimary, fontSize = 11.sp)
                }
                if (provider.id != "gemini_default") {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, null, tint = RoseError, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", color = RoseError, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderEditDialog(
    existing: ApiProviderEntity?,
    onDismiss: () -> Unit,
    onSave: (ApiProviderEntity) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "OpenAI Compatible") }
    var providerType by remember { mutableStateOf(existing?.providerType ?: "OPENAI") }
    var baseUrl by remember { mutableStateOf(existing?.baseUrl ?: "https://api.openai.com/v1") }
    var apiKey by remember { mutableStateOf(existing?.apiKey ?: "") }
    var selectedModel by remember { mutableStateOf(existing?.selectedModel ?: "gpt-4o-mini") }
    var priority by remember { mutableStateOf((existing?.priority ?: 3).toString()) }

    var keyVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Text(if (existing == null) "Add AI Provider" else "Edit Provider", color = TextPrimary)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Provider Type chips
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("GEMINI", "OPENAI", "ANTHROPIC", "OLLAMA").forEach { type ->
                        FilterChip(
                            selected = providerType == type,
                            onClick = {
                                providerType = type
                                when (type) {
                                    "GEMINI" -> {
                                        baseUrl = "https://generativelanguage.googleapis.com"
                                        selectedModel = "gemini-2.5-flash"
                                    }
                                    "OPENAI" -> {
                                        baseUrl = "https://api.openai.com/v1"
                                        selectedModel = "gpt-4o-mini"
                                    }
                                    "ANTHROPIC" -> {
                                        baseUrl = "https://api.anthropic.com/v1"
                                        selectedModel = "claude-3-5-sonnet-20241022"
                                    }
                                    "OLLAMA" -> {
                                        baseUrl = "http://localhost:11434"
                                        selectedModel = "hermes-3-llama-3.1"
                                    }
                                }
                            },
                            label = { Text(type, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary,
                                selectedLabelColor = TerminalBlack
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base Endpoint URL") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { keyVisible = !keyVisible }) {
                            Icon(
                                imageVector = if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = selectedModel,
                    onValueChange = { selectedModel = it },
                    label = { Text("Model Name") },
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
                onClick = {
                    val p = ApiProviderEntity(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        name = name,
                        providerType = providerType,
                        baseUrl = baseUrl,
                        apiKey = apiKey,
                        selectedModel = selectedModel,
                        priority = priority.toIntOrNull() ?: 3,
                        isEnabled = existing?.isEnabled ?: true,
                        requestCount = existing?.requestCount ?: 0
                    )
                    onSave(p)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("Save Provider", color = TerminalBlack, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
