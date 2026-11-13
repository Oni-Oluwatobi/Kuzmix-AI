package com.kuzmix.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kuzmix.ai.ui.theme.KuzmixBackground
import com.kuzmix.ai.ui.theme.KuzmixBorderSubtle
import com.kuzmix.ai.ui.theme.KuzmixOrange
import com.kuzmix.ai.ui.theme.KuzmixOrangeContainer
import com.kuzmix.ai.ui.theme.KuzmixOrangeDeep
import com.kuzmix.ai.ui.theme.KuzmixRadius
import com.kuzmix.ai.ui.theme.KuzmixSpacing
import com.kuzmix.ai.ui.theme.KuzmixTextPrimary
import com.kuzmix.ai.ui.theme.KuzmixTextSecondary
import com.kuzmix.ai.ui.theme.KuzmixTextOnAccent

@Composable
fun KuzmixTopBar(
    title: String,
    modifier: Modifier = Modifier,
    showBack: Boolean = false,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    subtitle: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBack && onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = KuzmixTextPrimary
                )
            }
        } else {
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = KuzmixTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            subtitle?.invoke()
        }

        actions()
    }
}

@Composable
fun KuzmixBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onCreateClick: () -> Unit
) {
    NavigationBar(
        containerColor = KuzmixBackground,
        tonalElevation = 0.dp,
        modifier = Modifier.background(KuzmixBackground)
    ) {
        val items = listOf(
            NavSpec("home", "Home", Icons.Outlined.Home),
            NavSpec("chats", "Chats", Icons.Outlined.ChatBubbleOutline),
            NavSpec("__create__", "", Icons.Filled.Add),
            NavSpec("tools", "Tools", Icons.Outlined.GridView),
            NavSpec("profile", "Profile", Icons.Outlined.Person)
        )

        items.forEach { item ->
            if (item.route == "__create__") {
                NavigationBarItem(
                    selected = false,
                    onClick = onCreateClick,
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(KuzmixOrangeDeep),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Create",
                                tint = KuzmixTextOnAccent
                            )
                        }
                    },
                    label = null,
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent
                    )
                )
            } else {
                val selected = currentRoute == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(item.route) },
                    icon = {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(KuzmixRadius.Pill))
                                .background(if (selected) KuzmixOrangeContainer else Color.Transparent)
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                item.icon,
                                contentDescription = item.label,
                                tint = if (selected) KuzmixOrange else KuzmixTextSecondary
                            )
                        }
                    },
                    label = {
                        Text(
                            item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) KuzmixOrange else KuzmixTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = KuzmixOrange,
                        unselectedIconColor = KuzmixTextSecondary,
                        selectedTextColor = KuzmixOrange,
                        unselectedTextColor = KuzmixTextSecondary,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}

private data class NavSpec(val route: String, val label: String, val icon: ImageVector)

@Composable
fun OfflineBadge(
    modifier: Modifier = Modifier,
    text: String = "Local AI"
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(KuzmixRadius.Pill))
            .background(KuzmixOrangeContainer)
            .border(1.dp, KuzmixOrange.copy(alpha = 0.3f), RoundedCornerShape(KuzmixRadius.Pill))
            .padding(horizontal = KuzmixSpacing.Sm, vertical = KuzmixSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(KuzmixOrange)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = KuzmixOrange
        )
    }
}
