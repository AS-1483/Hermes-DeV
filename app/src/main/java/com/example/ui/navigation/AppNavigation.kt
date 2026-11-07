package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.domain.model.DevelopmentDomain
import com.example.ui.components.HermesTopBar
import com.example.ui.screens.chat.AgentChatScreen
import com.example.ui.screens.permission.PermissionRequiredScreen
import com.example.ui.screens.providers.ApiManagerScreen
import com.example.ui.screens.skills.SkillStoreScreen
import com.example.ui.screens.telegram.TelegramConfigScreen
import com.example.ui.screens.tools.EnvironmentScreen
import com.example.ui.screens.workspace.WorkspaceScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.AgentViewModel
import com.example.util.PermissionHelper

@Composable
fun AppNavigation(
    viewModel: AgentViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var allPermissionsGranted by remember {
        mutableStateOf(PermissionHelper.areAllPermissionsGranted(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                allPermissionsGranted = PermissionHelper.areAllPermissionsGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Permission Required Gate: If permissions not allowed, block entry
    if (!allPermissionsGranted) {
        PermissionRequiredScreen(
            onAllPermissionsGranted = {
                allPermissionsGranted = true
            }
        )
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Chat.route

    val activeProject by viewModel.activeProject.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val currentSession by viewModel.currentSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val activeProvider = providers.firstOrNull { it.isEnabled }

    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showNewSessionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            HermesTopBar(
                activeProject = activeProject,
                projects = projects,
                onSelectProject = { viewModel.selectProject(it) },
                onOpenNewProjectDialog = { showNewProjectDialog = true },
                currentSession = currentSession,
                sessions = sessions,
                onSelectSession = { viewModel.selectSession(it) },
                onOpenNewSessionDialog = { showNewSessionDialog = true },
                onDeleteSession = { viewModel.deleteSession(it) },
                activeModelName = activeProvider?.selectedModel ?: "Hermes Local Core",
                onNavigateToProviders = {
                    navController.navigate(Screen.Providers.route) {
                        launchSingleTop = true
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CyberSurface,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar")
            ) {
                Screen.items.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 10.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TerminalBlack,
                            selectedTextColor = CyanPrimary,
                            indicatorColor = CyanPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        },
        containerColor = CyberDarkBg
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Chat.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Chat.route) {
                AgentChatScreen(viewModel = viewModel)
            }
            composable(Screen.Workspace.route) {
                WorkspaceScreen(viewModel = viewModel)
            }
            composable(Screen.Skills.route) {
                SkillStoreScreen(viewModel = viewModel)
            }
            composable(Screen.Environment.route) {
                EnvironmentScreen(viewModel = viewModel)
            }
            composable(Screen.Providers.route) {
                ApiManagerScreen(viewModel = viewModel)
            }
            composable(Screen.Telegram.route) {
                TelegramConfigScreen(viewModel = viewModel)
            }
        }
    }

    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismiss = { showNewProjectDialog = false },
            onCreate = { name, desc, domain ->
                viewModel.createProject(name, desc, domain)
                showNewProjectDialog = false
            }
        )
    }

    if (showNewSessionDialog) {
        NewSessionDialog(
            onDismiss = { showNewSessionDialog = false },
            onCreateSession = { title ->
                viewModel.createNewSession(title)
                showNewSessionDialog = false
            }
        )
    }
}

@Composable
fun NewSessionDialog(
    onDismiss: () -> Unit,
    onCreateSession: (String) -> Unit
) {
    var title by remember { mutableStateOf("New Development Thread") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddComment, null, tint = AmberAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start New Session", color = TextPrimary)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Create an isolated development session thread for this project:",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Session Title") },
                    placeholder = { Text("e.g. Bugfix: Camera preview / Auth API") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberAccent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) onCreateSession(title)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
            ) {
                Text("Create Session", color = TerminalBlack, fontWeight = FontWeight.Bold)
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
fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, DevelopmentDomain) -> Unit
) {
    var name by remember { mutableStateOf("My Software Project") }
    var desc by remember { mutableStateOf("Autonomous development project") }
    var selectedDomain by remember { mutableStateOf(DevelopmentDomain.ANDROID) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CreateNewFolder, null, tint = CyanPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Software Project", color = TextPrimary)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Development Domain:", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))

                Column {
                    DevelopmentDomain.values().forEach { domain ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = selectedDomain == domain,
                                onClick = { selectedDomain = domain },
                                colors = RadioButtonDefaults.colors(selectedColor = CyanPrimary)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(domain.title, color = TextPrimary, fontSize = 12.sp)
                                Text(domain.framework, color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, desc, selectedDomain) },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("Initialize Project", color = TerminalBlack)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
