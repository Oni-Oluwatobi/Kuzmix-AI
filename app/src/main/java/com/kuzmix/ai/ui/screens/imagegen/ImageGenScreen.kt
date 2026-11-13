package com.kuzmix.ai.ui.screens.imagegen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuzmix.ai.ui.components.KuzmixButton
import com.kuzmix.ai.ui.theme.KuzmixBackground
import com.kuzmix.ai.ui.theme.KuzmixBorder
import com.kuzmix.ai.ui.theme.KuzmixOrange
import com.kuzmix.ai.ui.theme.KuzmixRadius
import com.kuzmix.ai.ui.theme.KuzmixSpacing
import com.kuzmix.ai.ui.theme.KuzmixSurfaceElevated
import com.kuzmix.ai.ui.theme.KuzmixTextOnAccent
import com.kuzmix.ai.ui.theme.KuzmixTextPrimary
import com.kuzmix.ai.ui.theme.KuzmixTextSecondary
import com.kuzmix.ai.ui.theme.KuzmixTextTertiary

private val styles = listOf("Realistic", "Anime", "3D", "Cartoon", "Minimalist", "Artistic")
private val ratios = listOf("1:1", "3:4", "9:16", "16:9")

@Composable
fun ImageGenScreen(
    viewModel: ImageGenViewModel,
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
                text = "Image Generator",
                style = MaterialTheme.typography.titleLarge,
                color = KuzmixTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = KuzmixSpacing.ScreenHorizontal)
        ) {
            Spacer(modifier = Modifier.height(KuzmixSpacing.Md))

            Text(
                text = "Describe what you want to create...",
                style = MaterialTheme.typography.titleMedium,
                color = KuzmixTextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "e.g. A futuristic city at night, a lion in the forest, a minimalist logo, etc.",
                style = MaterialTheme.typography.bodySmall,
                color = KuzmixTextSecondary
            )

            Spacer(modifier = Modifier.height(KuzmixSpacing.Md))

            OutlinedTextField(
                value = uiState.prompt,
                onValueChange = viewModel::onPromptChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Describe your image...", color = KuzmixTextTertiary)
                },
                minLines = 4,
                maxLines = 6,
                trailingIcon = {
                    Text(
                        text = "${uiState.prompt.length}/500",
                        style = MaterialTheme.typography.labelSmall,
                        color = KuzmixTextTertiary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = KuzmixSurfaceElevated,
                    unfocusedContainerColor = KuzmixSurfaceElevated,
                    focusedBorderColor = KuzmixBorder,
                    unfocusedBorderColor = KuzmixBorder,
                    focusedTextColor = KuzmixTextPrimary,
                    unfocusedTextColor = KuzmixTextPrimary,
                    cursorColor = KuzmixOrange
                ),
                shape = RoundedCornerShape(KuzmixRadius.Medium)
            )

            Spacer(modifier = Modifier.height(KuzmixSpacing.Xxl))

            Text(
                text = "Choose Style",
                style = MaterialTheme.typography.titleSmall,
                color = KuzmixTextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(KuzmixSpacing.Md))

            Column(verticalArrangement = Arrangement.spacedBy(KuzmixSpacing.Sm)) {
                styles.chunked(3).forEach { rowStyles ->
                    Row(horizontalArrangement = Arrangement.spacedBy(KuzmixSpacing.Sm)) {
                        rowStyles.forEach { style ->
                            val selected = uiState.style == style
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (selected) KuzmixOrange
                                        else KuzmixSurfaceElevated
                                    )
                                    .border(
                                        width = if (selected) 0.dp else 1.dp,
                                        color = KuzmixBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { viewModel.onStyleChange(style) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = style,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) KuzmixTextOnAccent else KuzmixTextPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(KuzmixSpacing.Xxl))

            Text(
                text = "Aspect Ratio",
                style = MaterialTheme.typography.titleSmall,
                color = KuzmixTextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(KuzmixSpacing.Md))

            Row(horizontalArrangement = Arrangement.spacedBy(KuzmixSpacing.Sm)) {
                ratios.forEach { ratio ->
                    val selected = uiState.aspectRatio == ratio
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) KuzmixOrange.copy(alpha = 0.15f)
                                else KuzmixSurfaceElevated
                            )
                            .border(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) KuzmixOrange else KuzmixBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.onAspectRatioChange(ratio) }
                            .padding(vertical = KuzmixSpacing.Md),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Simple ratio visual
                        val (w, h) = when (ratio) {
                            "1:1" -> 1f to 1f
                            "3:4" -> 3f to 4f
                            "9:16" -> 9f to 16f
                            else -> 16f to 9f
                        }
                        Box(
                            modifier = Modifier
                                .size(width = 20.dp * w / maxOf(w, h) * 2f + 8.dp, height = 20.dp * h / maxOf(w, h) * 2f + 8.dp)
                                .border(1.5.dp, if (selected) KuzmixOrange else KuzmixTextTertiary, RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = ratio,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) KuzmixOrange else KuzmixTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(KuzmixSpacing.Xxl))

            if (uiState.isGenerating) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(KuzmixRadius.Medium))
                        .background(KuzmixSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = KuzmixOrange)
                }
                Spacer(modifier = Modifier.height(KuzmixSpacing.Lg))
            }

            if (uiState.statusMessage != null) {
                Text(
                    text = uiState.statusMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = KuzmixTextSecondary,
                    modifier = Modifier.padding(bottom = KuzmixSpacing.Md)
                )
            }

            KuzmixButton(
                text = "Generate",
                onClick = viewModel::generate,
                loading = uiState.isGenerating,
                leadingIcon = Icons.Filled.AutoAwesome
            )

            Spacer(modifier = Modifier.height(KuzmixSpacing.Huge))
        }
    }
}
