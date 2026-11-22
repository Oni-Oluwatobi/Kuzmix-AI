package com.kuzmix.ai.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kuzmix.ai.data.secret.StudyNotesOpener
import com.kuzmix.ai.domain.ai.GuideSegment
import com.kuzmix.ai.domain.ai.SecretDocs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
private fun AssistantBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    onCopied: () -> Unit = {}
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }

    // Admin study guides are read-only: strip the invisible marker and never
    // attach the copy action (or any long-press menu) to them.
    val locked = message.content.startsWith(SecretDocs.MARKER)
    val raw = if (locked) message.content.removePrefix(SecretDocs.MARKER) else message.content
    val studyMatch = Regex("\\[\\[study:([^\\]]+)]]").find(raw)
    val studyKey = studyMatch?.groupValues?.get(1)
    val body = if (studyMatch != null) raw.replace(studyMatch.value, "").trim('\n', ' ') else raw

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
                    .then(
                        if (locked) {
                            Modifier
                        } else {
                            Modifier.combinedClickable(
                                onClick = { if (body.isNotBlank()) menuOpen = true },
                                onLongClick = { if (body.isNotBlank()) menuOpen = true }
                            )
                        }
                    )
                    .padding(horizontal = KuzmixSpacing.Lg, vertical = KuzmixSpacing.Md)
            ) {
                if (!locked) {
                    Text(
                        text = body,
                        color = KuzmixTextPrimary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    // Guides may embed diagrams: prose and images in order.
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecretDocs.segments(body).forEach { segment ->
                            when (segment) {
                                is GuideSegment.Text ->
                                    if (segment.text.isNotBlank()) {
                                        Text(
                                            text = segment.text.trim('\n'),
                                            color = KuzmixTextPrimary,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                is GuideSegment.Image -> GuideImage(segment.asset)
                            }
                        }
                        if (studyKey != null) {
                            // builds the .docx study guide and opens it with
                            // the device's Office viewer (Word / WPS / Docs)
                            val scope = rememberCoroutineScope()
                            var preparing by remember { mutableStateOf(false) }
                            KuzmixButton(
                                text = if (preparing) "Preparing study notes…"
                                else "Open study notes · ${SecretDocs.labelFor(studyKey)}",
                                enabled = !preparing,
                                onClick = {
                                    preparing = true
                                    scope.launch {
                                        val ok = withContext(Dispatchers.IO) {
                                            try {
                                                StudyNotesOpener.open(context, studyKey)
                                            } catch (t: Throwable) {
                                                false
                                            }
                                        }
                                        preparing = false
                                        if (!ok) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "No document reader found — install Microsoft Word or WPS Office to open the study notes.",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = formatTime(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = KuzmixTextSecondary
            )
            if (!locked) {
                CopyMenu(
                    expanded = menuOpen,
                    onDismiss = { menuOpen = false },
                    onCopy = {
                        if (copyMessageToClipboard(context, body)) onCopied()
                    }
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

/**
 * Loads a bundled guide diagram from assets off the main thread and shows it
 * inside the locked guide bubble.
 */
@Composable
private fun GuideImage(assetPath: String) {
    val context = LocalContext.current
    var failed by remember(assetPath) { mutableStateOf(false) }
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, assetPath) {
        value = try {
            withContext(Dispatchers.IO) {
                context.assets.open(assetPath).use { BitmapFactory.decodeStream(it) }
            }
        } catch (t: Throwable) {
            failed = true
            null
        }
    }
    val bmp = bitmap
    when {
        bmp != null -> Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
        )
        failed -> Text(
            text = "[diagram: ${assetPath.substringAfterLast('/')}]",
            color = KuzmixTextSecondary,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
