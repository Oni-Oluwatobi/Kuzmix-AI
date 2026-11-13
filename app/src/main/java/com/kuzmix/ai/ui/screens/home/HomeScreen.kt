package com.kuzmix.ai.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuzmix.ai.domain.ai.KnowledgeHit
import com.kuzmix.ai.domain.model.Conversation
import com.kuzmix.ai.ui.components.KuzmixSectionHeader
import com.kuzmix.ai.ui.components.OfflineBadge
import com.kuzmix.ai.ui.theme.KuzmixBackground
import com.kuzmix.ai.ui.theme.KuzmixBorder
import com.kuzmix.ai.ui.theme.KuzmixOrange
import com.kuzmix.ai.ui.theme.KuzmixOrangeContainer
import com.kuzmix.ai.ui.theme.KuzmixRadius
import com.kuzmix.ai.ui.theme.KuzmixSpacing
import com.kuzmix.ai.ui.theme.KuzmixSurface
import com.kuzmix.ai.ui.theme.KuzmixSurfaceElevated
import com.kuzmix.ai.ui.theme.KuzmixTextOnAccent
import com.kuzmix.ai.ui.theme.KuzmixTextPrimary
import com.kuzmix.ai.ui.theme.KuzmixTextSecondary
import com.kuzmix.ai.ui.theme.KuzmixTextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    userName: String,
    conversations: List<Conversation>,
    offlineReady: Boolean,
    knowledgeHits: List<KnowledgeHit>,
    onSearchQuery: (String) -> Unit,
    onAskKnowledge: (String) -> Unit,
    onNewChat: () -> Unit,
    onOpenChat: (Long) -> Unit,
    onOpenImageGen: () -> Unit,
    onOpenCode: () -> Unit,
    onOpenTools: () -> Unit,
    onSeeAllChats: () -> Unit,
    onStudyPlan: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val query = searchQuery.trim()
    val visibleConversations = if (query.isEmpty()) {
        conversations
    } else {
        conversations.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.lastMessagePreview.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KuzmixBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KuzmixSpacing.ScreenHorizontal)
            .padding(top = KuzmixSpacing.Xxxl, bottom = KuzmixSpacing.Huge)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "Good morning,",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KuzmixTextSecondary
                )
                Text(
                    text = "$userName \u2600",
                    style = MaterialTheme.typography.headlineLarge,
                    color = KuzmixTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Explore 2,500+ concepts completely offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = KuzmixTextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(KuzmixSurface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.take(1),
                    style = MaterialTheme.typography.titleMedium,
                    color = KuzmixOrange,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (offlineReady) {
            Spacer(modifier = Modifier.height(KuzmixSpacing.Md))
            OfflineBadge(text = "Local AI  •  Offline Ready")
        }

        Spacer(modifier = Modifier.height(KuzmixSpacing.Xl))

        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(KuzmixSpacing.Md))
                .background(KuzmixSurfaceElevated)
                .padding(horizontal = KuzmixSpacing.Lg, vertical = KuzmixSpacing.Md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = KuzmixTextTertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(KuzmixSpacing.Md))
            BasicTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    onSearchQuery(it)
                },
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = KuzmixTextPrimary),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search anything...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KuzmixTextTertiary
                            )
                        }
                        innerTextField()
                    }
                }
            )
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = {
                        searchQuery = ""
                        onSearchQuery("")
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Clear search",
                        tint = KuzmixTextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Knowledge preview: what the local knowledge base has for this
        // query — tapping a hit asks the typed query in a new chat.
        if (query.isNotEmpty() && knowledgeHits.isNotEmpty()) {
            Spacer(modifier = Modifier.height(KuzmixSpacing.Xl))
            Text(
                text = "Knowledge",
                style = MaterialTheme.typography.titleSmall,
                color = KuzmixTextSecondary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(KuzmixSpacing.Md))
            knowledgeHits.forEach { hit ->
                KnowledgeHitItem(
                    hit = hit,
                    onClick = { onAskKnowledge(query) }
                )
                Spacer(modifier = Modifier.height(KuzmixSpacing.Md))
            }
        }

        Spacer(modifier = Modifier.height(KuzmixSpacing.Xl))

        // Start a new chat — orange pill CTA in the lower thumb zone.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(KuzmixRadius.Card))
                .background(KuzmixOrange)
                .clickable(onClick = onNewChat)
                .padding(KuzmixSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(KuzmixRadius.Small))
                    .background(KuzmixBackground.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("K", color = KuzmixTextOnAccent, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(KuzmixSpacing.Lg))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "New Conversation",
                    style = MaterialTheme.typography.titleMedium,
                    color = KuzmixTextOnAccent,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Ask anything, get instant answers",
                    style = MaterialTheme.typography.bodySmall,
                    color = KuzmixTextOnAccent.copy(alpha = 0.85f)
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = KuzmixTextOnAccent
            )
        }

        Spacer(modifier = Modifier.height(KuzmixSpacing.Xl))

        // Quick actions grid — four primary entry points.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KuzmixSpacing.Md)
        ) {
            QuickActionCard(
                icon = Icons.Filled.Add,
                title = "New Conversation",
                subtitle = "Start a fresh chat",
                modifier = Modifier.weight(1f),
                onClick = onNewChat
            )
            QuickActionCard(
                icon = Icons.Filled.Code,
                title = "Code Assistant",
                subtitle = "Write, debug and build code",
                modifier = Modifier.weight(1f),
                onClick = onOpenCode
            )
        }

        Spacer(modifier = Modifier.height(KuzmixSpacing.Md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KuzmixSpacing.Md)
        ) {
            QuickActionCard(
                icon = Icons.Outlined.School,
                title = "Study & Tools",
                subtitle = "Plan learning and use tools",
                modifier = Modifier.weight(1f),
                onClick = onOpenTools
            )
            QuickActionCard(
                icon = Icons.Filled.Image,
                title = "Offline Models",
                subtitle = "Browse local AI models",
                modifier = Modifier.weight(1f),
                onClick = onOpenImageGen
            )
        }

        Spacer(modifier = Modifier.height(KuzmixSpacing.SectionGap))

        // Recent Chats
        KuzmixSectionHeader(
            title = if (query.isEmpty()) "Recent Chats" else "Search Results",
            actionText = "See all  →",
            onAction = onSeeAllChats
        )

        Spacer(modifier = Modifier.height(KuzmixSpacing.Lg))

        if (visibleConversations.isEmpty()) {
            Text(
                text = if (query.isEmpty()) {
                    "No conversations yet. Start a new chat to begin."
                } else {
                    "No chats match \"$query\"."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = KuzmixTextTertiary
            )
        } else {
            val toShow = if (query.isEmpty()) visibleConversations.take(5) else visibleConversations
            toShow.forEach { conversation ->
                RecentChatItem(
                    conversation = conversation,
                    onClick = { onOpenChat(conversation.id) }
                )
                Spacer(modifier = Modifier.height(KuzmixSpacing.Md))
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(KuzmixSurface)
            .clickable(onClick = onClick)
            .padding(KuzmixSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(KuzmixSpacing.Md)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(KuzmixOrangeContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = KuzmixOrange,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = KuzmixTextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = KuzmixTextSecondary,
            maxLines = 2
        )
    }
}

@Composable
private fun RecentChatItem(
    conversation: Conversation,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KuzmixSurface)
            .clickable(onClick = onClick)
            .padding(KuzmixSpacing.Lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(KuzmixOrangeContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = KuzmixOrange,
                modifier = Modifier.size(18.dp)
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
            Text(
                text = conversation.lastMessagePreview.ifEmpty { "No messages yet" },
                style = MaterialTheme.typography.bodySmall,
                color = KuzmixTextSecondary,
                maxLines = 1
            )
        }

        Text(
            text = formatRelativeTime(conversation.updatedAt),
            style = MaterialTheme.typography.labelSmall,
            color = KuzmixTextTertiary
        )
    }
}

@Composable
private fun KnowledgeHitItem(
    hit: KnowledgeHit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KuzmixSurface)
            .clickable(onClick = onClick)
            .padding(KuzmixSpacing.Lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(KuzmixOrangeContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.School,
                contentDescription = null,
                tint = KuzmixOrange,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(KuzmixSpacing.Lg))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = hit.name,
                style = MaterialTheme.typography.titleSmall,
                color = KuzmixTextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = hit.summary.ifEmpty { "Tap to ask about this topic" },
                style = MaterialTheme.typography.bodySmall,
                color = KuzmixTextSecondary,
                maxLines = 2
            )
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / 60000
    val hours = diff / 3600000
    val days = diff / 86400000

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 2 -> "Yesterday"
        days < 7 -> "${days}d ago"
        else -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
