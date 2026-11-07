package com.example.ui.screens.permission

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.theme.*
import com.example.util.PermissionHelper

@Composable
fun PermissionRequiredScreen(
    onAllPermissionsGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasStorage by remember { mutableStateOf(PermissionHelper.hasStoragePermission(context)) }
    var hasNotifications by remember { mutableStateOf(PermissionHelper.hasNotificationPermission(context)) }
    var hasBattery by remember { mutableStateOf(PermissionHelper.hasBatteryOptimizationExemption(context)) }

    fun refreshPermissions() {
        hasStorage = PermissionHelper.hasStoragePermission(context)
        hasNotifications = PermissionHelper.hasNotificationPermission(context)
        hasBattery = PermissionHelper.hasBatteryOptimizationExemption(context)
        if (hasStorage && hasNotifications && hasBattery) {
            onAllPermissionsGranted()
        }
    }

    // Auto-refresh when returning from System Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Notification permission launcher for Android 13+
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotifications = isGranted
        refreshPermissions()
    }

    // Storage permission launcher for Android 10 and below
    val legacyStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStorage = isGranted
        refreshPermissions()
    }

    val allGranted = hasStorage && hasNotifications && hasBattery

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Shield / Security Header Icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CyanPrimary.copy(alpha = 0.15f))
                .border(1.5.dp, CyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "System Authorizations",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Hermes Dev Agent executes real software development, shell pipelines, and APK builds. Grant all required permissions to unlock the development workspace.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Permission Card 1: Storage
        PermissionItemCard(
            title = "Full Storage Access (All Files)",
            description = "Enables Hermes to create workspaces, read and modify code files, compile Gradle dependencies, and export verified APKs.",
            icon = Icons.Default.FolderSpecial,
            isGranted = hasStorage,
            onGrant = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = PermissionHelper.getStorageSettingsIntent(context)
                    context.startActivity(intent)
                } else {
                    legacyStorageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Permission Card 2: Background Battery Optimization
        PermissionItemCard(
            title = "Unrestricted Background Execution",
            description = "Prevents Android OS from terminating long-running agent pipelines, test suites, or compilation processes when the app is in the background.",
            icon = Icons.Default.Bolt,
            isGranted = hasBattery,
            onGrant = {
                val intent = PermissionHelper.getBatteryOptimizationIntent(context)
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Permission Card 3: Notifications
        PermissionItemCard(
            title = "Task & Approval Notifications",
            description = "Alerts you promptly when an autonomous build finishes, test suites complete, or when a sensitive shell command requires your approval.",
            icon = Icons.Default.NotificationsActive,
            isGranted = hasNotifications,
            onGrant = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    hasNotifications = true
                    refreshPermissions()
                }
            }
        )

        Spacer(modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.height(24.dp))

        // Access Gate Button
        if (allGranted) {
            Button(
                onClick = onAllPermissionsGranted,
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("enter_workspace_button")
            ) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = TerminalBlack,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "All Granted — Enter Workspace",
                    color = TerminalBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        } else {
            Surface(
                color = CyberSurface,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, RoseError.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = RoseError,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Access Restricted: No Entry",
                            color = RoseError,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Please authorize all 3 permissions above to use the Hermes Dev Agent.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { refreshPermissions() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Icon(Icons.Default.Refresh, null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Re-check Permissions Status", color = CyanPrimary, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
fun PermissionItemCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    val borderColor by animateColorAsState(
        if (isGranted) EmeraldSuccess.copy(alpha = 0.5f) else AmberAccent.copy(alpha = 0.4f)
    )

    Surface(
        color = CyberCard,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isGranted) EmeraldSuccess.copy(alpha = 0.15f) else AmberAccent.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isGranted) EmeraldSuccess else AmberAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                if (isGranted) {
                    Surface(
                        color = EmeraldSuccess.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Check, null, tint = EmeraldSuccess, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Allowed", color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(
                        onClick = onGrant,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Grant", color = TerminalBlack, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}
