package com.kuzmix.ai.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kuzmix.ai.domain.model.ChatMessage
import com.kuzmix.ai.ui.theme.KuzmixAssistantBubble
import com.kuzmix.ai.ui.theme.KuzmixBorderSubtle
import com.kuzmix.ai.ui.theme.KuzmixOrange
import com.kuzmix.ai.ui.theme.KuzmixSpacing
import com.kuzmix.ai.ui.theme.KuzmixTextOnAccent
import com.kuzmix.ai.ui.theme.KuzmixTextPrimary
import com.kuzmix.ai.ui.theme.KuzmixTextSecondary

@Composable
fun KuzmixChatBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    onCopied: () -> Unit = {}
) {
    when (message.role) {
        ChatMessage.Role.USER -> UserBubble(message, modifier, onCopied)
        ChatMessage.Role.ASSISTANT -> AssistantBubble(message, modifier, onCopied)
    }
}

/**
 * Copies the raw message text — nothing else (no timestamps, labels, or
 * decorations) — to the native Android clipboard. Empty text is a no-op.
 */
private fun copyMessageToClipboard(context: Context, text: String): Boolean {
    if (text.isBlank()) return false
    return try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return false
        clipboard.setPrimaryClip(ClipData.newPlainText("Kuzmix AI message", text))
        true
    } catch (_: Exception) {
        false
    }
}

@Composable
private fun CopyMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Copy") },
            leadingIcon = {
                Icon(Icons.Filled.ContentCopy, contentDescription = null)
            },
            onClick = {
                onDismiss()
                onCopy()
            }
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun UserBubble(message: ChatMessage, modifier: Modifier = Modifier, onCopied: () -> Unit = {}) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp))
                    .background(KuzmixOrange)
                    .combinedClickable(
                        onClick = { if (message.content.isNotBlank()) menuOpen = true },
                        onLongClick = { if (message.content.isNotBlank()) menuOpen = true }
                    )
                    .padding(horizontal = KuzmixSpacing.Lg, vertical = KuzmixSpacing.Md)
            ) {
                Text(
                    text = message.content,
                    color = KuzmixTextOnAccent,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = formatTime(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = KuzmixTextSecondary
            )
            CopyMenu(
                expanded = menuOpen,
                onDismiss = { menuOpen = false },
                onCopy = {
                    if (copyMessageToClipboard(context, message.content)) onCopied()
                }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun AssistantBubble(message: ChatMessage, modifier: Modifier = Modifier, onCopied: () -> Unit = {}) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(KuzmixOrange.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "K",
                color = KuzmixOrange,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(KuzmixSpacing.Md))

        Column(modifier = Modifier.widthIn(max = 310.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                    .background(KuzmixAssistantBubble)
                    .border(1.dp, KuzmixBorderSubtle, RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                    .combinedClickable(
                        onClick = { if (message.content.isNotBlank()) menuOpen = true },
                        onLongClick = { if (message.content.isNotBlank()) menuOpen = true }
                    )
                    .padding(horizontal = KuzmixSpacing.Lg, vertical = KuzmixSpacing.Md)
            ) {
                Text(
                    text = message.content,
                    color = KuzmixTextPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = formatTime(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = KuzmixTextSecondary
            )
            CopyMenu(
                expanded = menuOpen,
                onDismiss = { menuOpen = false },
                onCopy = {
                    if (copyMessageToClipboard(context, message.content)) onCopied()
                }
            )
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
