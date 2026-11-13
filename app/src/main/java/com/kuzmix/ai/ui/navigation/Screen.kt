package com.kuzmix.ai.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object Home : Screen("home")
    data object Chats : Screen("chats")
    data object Tools : Screen("tools")
    data object Profile : Screen("profile")
    data object Chat : Screen("chat/{conversationId}") {
        fun create(conversationId: Long) = "chat/$conversationId"
    }
    data object ImageGen : Screen("image_gen")
    data object CodeAssistant : Screen("code_assistant")
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val isCreate: Boolean = false
)

val bottomNavItems = listOf(
    BottomNavItem("home", "Home", Icons.Outlined.Home, Icons.Filled.Home),
    BottomNavItem("chats", "Chats", Icons.Outlined.ChatBubbleOutline, Icons.Outlined.ChatBubbleOutline),
    BottomNavItem("__create__", "", Icons.Filled.Add, Icons.Filled.Add, isCreate = true),
    BottomNavItem("tools", "Tools", Icons.Outlined.GridView, Icons.Filled.GridView),
    BottomNavItem("profile", "Profile", Icons.Outlined.Person, Icons.Filled.Person)
)
