package com.kuzmix.ai.ui.screens.code

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuzmix.ai.ui.components.KuzmixChatBubble
import com.kuzmix.ai.ui.theme.KuzmixBackground
import com.kuzmix.ai.ui.theme.KuzmixBorder
import com.kuzmix.ai.ui.theme.KuzmixOrange
import com.kuzmix.ai.ui.theme.KuzmixOrangeContainer
import com.kuzmix.ai.ui.theme.KuzmixSpacing
import com.kuzmix.ai.ui.theme.KuzmixSurface
import com.kuzmix.ai.ui.theme.KuzmixSurfaceElevated
import com.kuzmix.ai.ui.theme.KuzmixTextOnAccent
import com.kuzmix.ai.ui.theme.KuzmixTextPrimary
import com.kuzmix.ai.ui.theme.KuzmixTextSecondary
import com.kuzmix.ai.ui.theme.KuzmixTextTertiary

private val languages = listOf(
    "Python" to "Py",
    "TypeScript" to "TS",
    "Java" to "J",
    "C++" to "C+",
    "SQL" to "SQL",
    "Android (Kotlin)" to "K",
    "JavaScript" to "JS",
    "HTML" to "5",
    "Other" to "..."
)

private val quickPrompts = listOf(
    "Debug Error",
    "Explain Architecture",
    "Write Unit Test",
    "Optimize Query"
)

@Composable
fun CodeAssistantScreen(
    viewModel: CodeViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KuzmixBackground)
            .verticalScroll(rememberScrollState())
    ) {
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
            Text(
                text = "Code Assistant",
                style = MaterialTheme.typography.titleLarge,
                color = KuzmixTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(modifier = Modifier.padding(horizontal = KuzmixSpacing.ScreenHorizontal)) {
            // Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(KuzmixSpacing.Md)
            ) {
                CodeTab(
                    text = "New Chat",
                    selected = uiState.activeTab == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onTabChange(0) }
                )
                CodeTab(
                    text = "My Projects",
                    selected = uiState.activeTab == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onTabChange(1) }
                )
            }

            Spacer(modifier = Modifier.height(KuzmixSpacing.Xl))

            Text(
                text = "Select Language",
                style = MaterialTheme.typography.titleSmall,
                color = KuzmixTextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(KuzmixSpacing.Md))

            Column(verticalArrangement = Arrangement.spacedBy(KuzmixSpacing.Sm)) {
                languages.chunked(3).forEach { rowLangs ->
                    Row(horizontalArrangement = Arrangement.spacedBy(KuzmixSpacing.Sm)) {
                        rowLangs.forEach { (name, badge) ->
                            val selected = uiState.selectedLanguage == name
                            LanguageChip(
                                name = name,
                                badge = badge,
                                selected = selected,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.onLanguageSelected(name) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(KuzmixSpacing.Xl))

            Text(
                text = "Quick Prompts",
                style = MaterialTheme.typography.titleSmall,
                color = KuzmixTextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(KuzmixSpacing.Md))

            quickPrompts.forEach { prompt ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(KuzmixSurface)
                        .clickable { viewModel.useQuickPrompt(prompt) }
                        .padding(horizontal = KuzmixSpacing.Lg, vertical = KuzmixSpacing.Md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = KuzmixTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = KuzmixTextTertiary
                    )
                }
                Spacer(modifier = Modifier.height(KuzmixSpacing.Sm))
            }

            // Messages area
            if (uiState.messages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(KuzmixSpacing.Xl))
                uiState.messages.forEach { msg ->
                    KuzmixChatBubble(message = msg)
                    Spacer(modifier = Modifier.height(KuzmixSpacing.Md))
                }
            }

            if (uiState.isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = KuzmixOrange,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyzing...", style = MaterialTheme.typography.bodySmall, color = KuzmixTextSecondary)
                }
                Spacer(modifier = Modifier.height(KuzmixSpacing.Md))
            }

            Spacer(modifier = Modifier.height(KuzmixSpacing.Lg))

            // Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = KuzmixSpacing.Xxl),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = uiState.input,
                    onValueChange = viewModel::onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("Ask about your code...", color = KuzmixTextTertiary)
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

                IconButton(
                    onClick = {
                        if (uiState.input.isNotBlank()) viewModel.sendPrompt(uiState.input.trim())
                    },
                    enabled = uiState.input.isNotBlank() && !uiState.isLoading,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (uiState.input.isNotBlank() && !uiState.isLoading) KuzmixOrange
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
    }
}

@Composable
private fun CodeTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) KuzmixOrange else Color.Transparent)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = KuzmixBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) KuzmixTextOnAccent else KuzmixTextSecondary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun LanguageChip(
    name: String,
    badge: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) KuzmixOrange.copy(alpha = 0.12f)
                else KuzmixSurface
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) KuzmixOrange else KuzmixBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = KuzmixSpacing.Md, horizontal = KuzmixSpacing.Sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) KuzmixOrange else KuzmixOrangeContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) KuzmixTextOnAccent else KuzmixOrange,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) KuzmixOrange else KuzmixTextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
