package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Chat : Screen("chat", "Agent Chat", Icons.Default.Chat)
    data object Workspace : Screen("workspace", "Workspace", Icons.Default.Code)
    data object Skills : Screen("skills", "Skill Store", Icons.Default.Extension)
    data object Environment : Screen("environment", "Toolchain", Icons.Default.Memory)
    data object Providers : Screen("providers", "API Keys", Icons.Default.VpnKey)
    data object Telegram : Screen("telegram", "Telegram Bot", Icons.Default.Send)

    companion object {
        val items: List<Screen>
            get() = listOf(Chat, Workspace, Skills, Environment, Providers, Telegram)
    }
}
