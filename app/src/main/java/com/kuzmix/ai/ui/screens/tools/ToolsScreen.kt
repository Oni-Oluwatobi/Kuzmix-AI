package com.kuzmix.ai.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kuzmix.ai.tools.GrammarChecker
import com.kuzmix.ai.tools.GrammarIssue
import com.kuzmix.ai.ui.components.KuzmixToolItem
import com.kuzmix.ai.ui.theme.KuzmixBackground
import com.kuzmix.ai.ui.theme.KuzmixOrange
import com.kuzmix.ai.ui.theme.KuzmixSpacing
import com.kuzmix.ai.ui.theme.KuzmixSuccess
import com.kuzmix.ai.ui.theme.KuzmixSurface
import com.kuzmix.ai.ui.theme.KuzmixTextPrimary
import com.kuzmix.ai.ui.theme.KuzmixTextSecondary
import com.kuzmix.ai.ui.theme.KuzmixTextTertiary
import kotlinx.coroutines.launch

@Composable
fun ToolsScreen(
    onOpenImageGen: () -> Unit,
    onOpenCode: () -> Unit,
    onOpenSummarizer: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showGrammarDialog by remember { mutableStateOf(false) }

    Column(
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
                text = "Tools",
                style = MaterialTheme.typography.headlineMedium,
                color = KuzmixTextPrimary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Powerful tools for your everyday needs",
                style = MaterialTheme.typography.bodyMedium,
                color = KuzmixTextSecondary
            )

            Spacer(modifier = Modifier.height(KuzmixSpacing.Xl))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(KuzmixSpacing.Md),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                item {
                    KuzmixToolItem(
                        icon = Icons.Filled.Image,
                        title = "Image Generator",
                        subtitle = "Offline stub / future local model",
                        onClick = onOpenImageGen
                    )
                }
                item {
                    KuzmixToolItem(
                        icon = Icons.Filled.Code,
                        title = "Code Assistant",
                        subtitle = "Get help with your code, fix errors, and more",
                        onClick = onOpenCode
                    )
                }
                item {
                    KuzmixToolItem(
                        icon = Icons.Outlined.DocumentScanner,
                        title = "Summarizer",
                        subtitle = "Summarize articles, docs and long text",
                        onClick = onOpenSummarizer
                    )
                }
                item {
                    KuzmixToolItem(
                        icon = Icons.Outlined.Translate,
                        title = "Translator",
                        subtitle = "Offline stub / future local model",
                        onClick = onOpenImageGen
                    )
                }
                item {
                    KuzmixToolItem(
                        icon = Icons.Outlined.Edit,
                        title = "Grammar Check",
                        subtitle = "Improve your writing offline",
                        onClick = { showGrammarDialog = true }
                    )
                }
                item {
                    KuzmixToolItem(
                        icon = Icons.Outlined.Widgets,
                        title = "More Tools",
                        subtitle = "Explore more useful tools coming soon",
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("More tools are on the way.")
                            }
                        }
                    )
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState)
    }

    if (showGrammarDialog) {
        GrammarCheckDialog(onDismiss = { showGrammarDialog = false })
    }
}

@Composable
private fun GrammarCheckDialog(onDismiss: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var issues by remember { mutableStateOf<List<GrammarIssue>>(emptyList()) }
    var hasRun by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KuzmixSurface,
        title = {
            Text(
                text = "Grammar Check",
                style = MaterialTheme.typography.titleMedium,
                color = KuzmixTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(KuzmixSpacing.Md)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        hasRun = false
                        copied = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Paste or type your text...", color = KuzmixTextTertiary)
                    },
                    minLines = 3,
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = KuzmixBackground,
                        unfocusedContainerColor = KuzmixBackground,
                        focusedBorderColor = KuzmixOrange,
                        unfocusedBorderColor = KuzmixOrange.copy(alpha = 0.4f),
                        focusedTextColor = KuzmixTextPrimary,
                        unfocusedTextColor = KuzmixTextPrimary,
                        cursorColor = KuzmixOrange
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                if (hasRun) {
                    when {
                        input.isBlank() -> Text(
                            text = "Enter some text to check.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KuzmixTextSecondary
                        )
                        issues.isEmpty() -> Text(
                            text = "No issues found.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KuzmixSuccess
                        )
                        else -> issues.forEach { issue ->
                            Text(
                                text = "• ${issue.message} — ${issue.suggestion}",
                                style = MaterialTheme.typography.bodySmall,
                                color = KuzmixTextSecondary
                            )
                        }
                    }
                }

                if (copied) {
                    Text(
                        text = "Fixed text copied to clipboard.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KuzmixSuccess
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (input.isBlank()) {
                        hasRun = true
                        issues = emptyList()
                    } else {
                        issues = GrammarChecker.check(input)
                        hasRun = true
                    }
                    copied = false
                }
            ) {
                Text("Check", color = KuzmixOrange)
            }
        },
        dismissButton = {
            Row {
                if (hasRun && input.isNotBlank() && issues.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(GrammarChecker.correct(input)))
                            copied = true
                        }
                    ) {
                        Text("Copy fixed", color = KuzmixTextSecondary)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", color = KuzmixTextSecondary)
                }
            }
        }
    )
}
