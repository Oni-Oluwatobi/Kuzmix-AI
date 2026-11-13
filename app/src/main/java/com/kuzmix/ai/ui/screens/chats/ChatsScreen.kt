package com.kuzmix.ai.ui.screens.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuzmix.ai.domain.model.Conversation
import com.kuzmix.ai.ui.theme.KuzmixBackground
import com.kuzmix.ai.ui.theme.KuzmixOrange
import com.kuzmix.ai.ui.theme.KuzmixOrangeContainer
import com.kuzmix.ai.ui.theme.KuzmixRadius
import com.kuzmix.ai.ui.theme.KuzmixSpacing
import com.kuzmix.ai.ui.theme.KuzmixSurface
import com.kuzmix.ai.ui.theme.KuzmixSurfaceElevated
import com.kuzmix.ai.ui.theme.KuzmixTextPrimary
import com.kuzmix.ai.ui.theme.KuzmixTextSecondary
import com.kuzmix.ai.ui.theme.KuzmixTextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatsScreen(
    viewModel: ChatsViewModel,
    onOpenChat: (Long) -> Unit,
    onNewChat: () -> Unit
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    val query = searchQuery.trim()
    val visible = if (query.isEmpty()) conversations else conversations.filter {
        it.title.contains(query, ignoreCase = true) ||
            it.lastMessagePreview.contains(query, ignoreCase = true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KuzmixBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = KuzmixSpacing.ScreenHorizontal)
                .padding(top = KuzmixSpacing.Xxxl)
        ) {
            Text(
                text = "Chats",
                style = MaterialTheme.typography.headlineMedium,
                color = KuzmixTextPrimary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Your conversations, stored locally",
                style = MaterialTheme.typography.bodyMedium,
                color = KuzmixTextSecondary
            )

            Spacer(modifier = Modifier.height(KuzmixSpacing.Lg))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(KuzmixRadius.Small))
                    .background(KuzmixSurfaceElevated)
                    .padding(horizontal = KuzmixSpacing.Lg, vertical = KuzmixSpacing.Md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = KuzmixTextTertiary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(KuzmixSpacing.Md))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = KuzmixTextPrimary),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search chats...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = KuzmixTextTertiary
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(KuzmixSpacing.Xl))

            if (visible.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(KuzmixOrangeContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            tint = KuzmixOrange,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(KuzmixSpacing.Lg))
                    Text(
                        text = "No conversations yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = KuzmixTextPrimary
                    )
                    Text(
                        text = "Start a new chat to begin learning",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KuzmixTextSecondary
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(KuzmixSpacing.Md),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(visible, key = { it.id }) { conversation ->
                        ChatListItem(
                            conversation = conversation,
                            onClick = { onOpenChat(conversation.id) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onNewChat,
            containerColor = KuzmixOrange,
            contentColor = KuzmixBackground,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(KuzmixSpacing.ScreenHorizontal)
                .padding(bottom = 80.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "New chat")
        }
    }
}

@Composable
private fun ChatListItem(
    conversation: Conversation,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(KuzmixSurface)
            .clickable(onClick = onClick)
            .padding(KuzmixSpacing.Lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(KuzmixOrangeContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = KuzmixOrange,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(KuzmixSpacing.Lg))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.title,
                style = MaterialTheme.typography.titleSmall,
                color = KuzmixTextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (conversation.isCodeMode) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(KuzmixRadius.Pill))
                            .background(KuzmixOrangeContainer)
                            .padding(horizontal = KuzmixSpacing.Sm, vertical = 1.dp)
                    ) {
                        Text(
                            text = "Code",
                            style = MaterialTheme.typography.labelSmall,
                            color = KuzmixOrange
                        )
                    }
                    Spacer(modifier = Modifier.width(KuzmixSpacing.Sm))
                }
                Text(
                    text = conversation.lastMessagePreview.ifEmpty { "No messages yet" },
                    style = MaterialTheme.typography.bodySmall,
                    color = KuzmixTextSecondary,
                    maxLines = 1
                )
            }
        }

        Text(
            text = formatTime(conversation.updatedAt),
            style = MaterialTheme.typography.labelSmall,
            color = KuzmixTextTertiary
        )
    }
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val days = (now - timestamp) / 86400000
    return when {
        days < 1 -> {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
        days < 2 -> "Yesterday"
        else -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
