package com.kuzmix.ai.ui.screens.chat

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuzmix.ai.ui.components.KuzmixChatBubble
import com.kuzmix.ai.ui.theme.KuzmixBackground
import com.kuzmix.ai.ui.theme.KuzmixBorder
import com.kuzmix.ai.ui.theme.KuzmixOrange
import com.kuzmix.ai.ui.theme.KuzmixSpacing
import com.kuzmix.ai.ui.theme.KuzmixSurfaceElevated
import com.kuzmix.ai.ui.theme.KuzmixSuccess
import com.kuzmix.ai.ui.theme.KuzmixTextPrimary
import com.kuzmix.ai.ui.theme.KuzmixTextSecondary
import com.kuzmix.ai.ui.theme.KuzmixTextTertiary
import com.kuzmix.ai.voice.SpeechInputController
import com.kuzmix.ai.voice.SpeechInputState
import kotlinx.coroutines.launch

private fun Context.findActivity(): android.app.Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is android.app.Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    conversationId: Long,
    initialMessage: String? = null,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val speechController = remember(context) {
        SpeechInputController(context) { recognized ->
            inputText = SpeechInputController.mergeDictation(inputText, recognized)
        }
    }
    val speechState by speechController.state.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val permanently = !granted &&
            context.findActivity()?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) == false
        speechController.onPermissionResult(granted, permanently)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, speechController) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) speechController.stop()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            speechController.release()
        }
    }

    LaunchedEffect(speechState) {
        when (val state = speechState) {
            is SpeechInputState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                speechController.clearTransient()
            }
            is SpeechInputState.Unavailable -> {
                snackbarHostState.showSnackbar(state.message)
                speechController.clearTransient()
            }
            is SpeechInputState.PermissionDenied -> {
                snackbarHostState.showSnackbar(
                    if (state.permanently) {
                        "Microphone permission is permanently denied. Enable it in Settings."
                    } else {
                        "Microphone permission denied."
                    }
                )
                speechController.clearTransient()
            }
            else -> Unit
        }
    }

    LaunchedEffect(conversationId) {
        viewModel.loadConversation(conversationId, initialMessage)
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KuzmixBackground)
                .imePadding()
        ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = KuzmixTextPrimary
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(KuzmixOrange.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("K", color = KuzmixOrange, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(KuzmixSpacing.Md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = uiState.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = KuzmixTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(KuzmixSuccess)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = uiState.engineLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = KuzmixTextSecondary
                    )
                }
            }

            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "More",
                        tint = KuzmixTextPrimary
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete chat") },
                        leadingIcon = {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                        },
                        onClick = {
                            menuOpen = false
                            viewModel.deleteActiveConversation()
                            onBack()
                        }
                    )
                }
            }
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = KuzmixSpacing.ScreenHorizontal,
                vertical = KuzmixSpacing.Lg
                ),
            verticalArrangement = Arrangement.spacedBy(KuzmixSpacing.Lg)
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                KuzmixChatBubble(
                    message = message,
                    onCopied = {
                        scope.launch { snackbarHostState.showSnackbar("Copied to clipboard") }
                    }
                )
            }

            val partial = uiState.streamingText
            if (partial != null) {
                item(key = "streaming") {
                    KuzmixChatBubble(
                        message = com.kuzmix.ai.domain.model.ChatMessage(
                            id = -1L,
                            role = com.kuzmix.ai.domain.model.ChatMessage.Role.ASSISTANT,
                            content = partial
                        ),
                        onCopied = {}
                    )
                }
            }

            if (uiState.isLoading && partial == null) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = KuzmixOrange,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(KuzmixSpacing.Md))
                        Text(
                            text = "Thinking...",
                            style = MaterialTheme.typography.bodySmall,
                            color = KuzmixTextSecondary
                        )
                    }
                }
            }
        }

        // Tappable chips — quiz wizard steps (class/term/subject/topic)
        // and MCQ answer letters. Tapping one sends it as a message; they
        // never show while an answer streams (no stale tap targets).
        if (uiState.chips.isNotEmpty() && !uiState.isLoading) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KuzmixSpacing.ScreenHorizontal)
                    .padding(bottom = KuzmixSpacing.Sm),
                horizontalArrangement = Arrangement.spacedBy(KuzmixSpacing.Sm),
                verticalArrangement = Arrangement.spacedBy(KuzmixSpacing.Xs)
            ) {
                uiState.chips.forEach { chip ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = KuzmixSurfaceElevated,
                        border = BorderStroke(1.dp, KuzmixOrange)
                    ) {
                        Text(
                            text = chip,
                            style = MaterialTheme.typography.labelMedium,
                            color = KuzmixOrange,
                            modifier = Modifier
                                .clickable { viewModel.sendMessage(chip) }
                                .padding(
                                    horizontal = KuzmixSpacing.Md,
                                    vertical = KuzmixSpacing.Sm
                                )
                        )
                    }
                }
            }
        }

        // Composer — floating capsule above the IME + system bars.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KuzmixSpacing.ScreenHorizontal)
                .windowInsetsPadding(WindowInsets.ime)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
                .padding(bottom = KuzmixSpacing.Sm, top = KuzmixSpacing.Sm),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "Attachments aren't available yet — Kuzmix runs fully offline for now."
                    )
                }
            }) {
                Icon(
                    Icons.Filled.AttachFile,
                    contentDescription = "Attach",
                    tint = KuzmixTextTertiary
                )
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                placeholder = {
                    Text(
                        text = if (speechState is SpeechInputState.Listening) {
                            "Listening..."
                        } else {
                            "Message Kuzmix AI..."
                        },
                        color = if (speechState is SpeechInputState.Listening) KuzmixOrange else KuzmixTextTertiary
                    )
                },
                minLines = 1,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = KuzmixSurfaceElevated,
                    unfocusedContainerColor = KuzmixSurfaceElevated,
                    focusedBorderColor = KuzmixBorder,
                    unfocusedBorderColor = KuzmixBorder,
                    focusedTextColor = KuzmixTextPrimary,
                    unfocusedTextColor = KuzmixTextPrimary,
                    cursorColor = KuzmixOrange
                ),
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(modifier = Modifier.width(KuzmixSpacing.Sm))

            // Offline voice dictation: tap to start, tap again to stop.
            IconButton(
                onClick = {
                    when (speechState) {
                        SpeechInputState.Listening, SpeechInputState.Processing -> speechController.stop()
                        else -> {
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                speechController.start()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    }
                }
            ) {
                if (speechState is SpeechInputState.Processing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = KuzmixOrange,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = if (speechState is SpeechInputState.Listening) {
                            "Stop voice input"
                        } else {
                            "Voice input"
                        },
                        tint = if (speechState is SpeechInputState.Listening) KuzmixOrange else KuzmixTextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.width(KuzmixSpacing.Sm))

            IconButton(
                onClick = {
                    if (inputText.isNotBlank() && !uiState.isLoading) {
                        viewModel.sendMessage(inputText.trim())
                        inputText = ""
                    }
                },
                enabled = inputText.isNotBlank() && !uiState.isLoading,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputText.isNotBlank() && !uiState.isLoading) KuzmixOrange
                        else KuzmixOrange.copy(alpha = 0.4f)
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = KuzmixBackground
                )
            }
        }
    }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}