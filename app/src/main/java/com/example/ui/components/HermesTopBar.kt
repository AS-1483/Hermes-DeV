package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ChatSessionEntity
import com.example.data.local.entity.ProjectEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HermesTopBar(
    activeProject: ProjectEntity?,
    projects: List<ProjectEntity>,
    onSelectProject: (ProjectEntity) -> Unit,
    onOpenNewProjectDialog: () -> Unit,
    currentSession: ChatSessionEntity?,
    sessions: List<ChatSessionEntity>,
    onSelectSession: (ChatSessionEntity) -> Unit,
    onOpenNewSessionDialog: () -> Unit,
    onDeleteSession: (String) -> Unit,
    activeModelName: String,
    onNavigateToProviders: () -> Unit
) {
    var projectMenuExpanded by remember { mutableStateOf(false) }
    var sessionMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = CyberSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardStroke)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Brand
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyanPrimary.copy(alpha = 0.15f))
                            .border(1.dp, CyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "Hermes Logo",
                            tint = CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Hermes Dev",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AmberAccent.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "AGENT",
                                    color = AmberAccent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Autonomous AI Software Engineer",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Provider Badge
                Surface(
                    color = CyberCard,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardStroke),
                    modifier = Modifier.clickable { onNavigateToProviders() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldSuccess)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = activeModelName,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Project & Session Selector Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Project Selector
                    Box {
                        Surface(
                            color = CyberDarkBg,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardStroke),
                            modifier = Modifier.clickable { projectMenuExpanded = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = activeProject?.name ?: "No Project",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Dropdown",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = projectMenuExpanded,
                            onDismissRequest = { projectMenuExpanded = false },
                            modifier = Modifier.background(CyberSurface)
                        ) {
                            projects.forEach { proj ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(proj.name, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                            Text(proj.domain, color = CyanPrimary, fontSize = 11.sp)
                                        }
                                    },
                                    onClick = {
                                        onSelectProject(proj)
                                        projectMenuExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider(color = CyberCardStroke)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Add, null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Create New Project", color = CyanPrimary)
                                    }
                                },
                                onClick = {
                                    projectMenuExpanded = false
                                    onOpenNewProjectDialog()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Session Selector
                    Box {
                        Surface(
                            color = CyberDarkBg,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable { sessionMenuExpanded = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forum,
                                    contentDescription = null,
                                    tint = AmberAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = currentSession?.title?.take(16) ?: "Session",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Session Dropdown",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = sessionMenuExpanded,
                            onDismissRequest = { sessionMenuExpanded = false },
                            modifier = Modifier.background(CyberSurface)
                        ) {
                            Text(
                                text = "Project Chat Sessions",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                            sessions.forEach { sess ->
                                val isCurrent = sess.id == currentSession?.id
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = sess.title,
                                                color = if (isCurrent) CyanPrimary else TextPrimary,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (sessions.size > 1) {
                                                IconButton(
                                                    onClick = { onDeleteSession(sess.id) },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Delete Session",
                                                        tint = RoseError,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onClick = {
                                        onSelectSession(sess)
                                        sessionMenuExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider(color = CyberCardStroke)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AddComment, null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("+ Add New Session", color = AmberAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                },
                                onClick = {
                                    sessionMenuExpanded = false
                                    onOpenNewSessionDialog()
                                }
                            )
                        }
                    }
                }

                if (activeProject != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AltRoute,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = activeProject.gitBranch,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
