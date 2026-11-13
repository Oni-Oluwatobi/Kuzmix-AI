package com.kuzmix.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kuzmix.ai.ui.theme.KuzmixBorder
import com.kuzmix.ai.ui.theme.KuzmixRadius
import com.kuzmix.ai.ui.theme.KuzmixSpacing
import com.kuzmix.ai.ui.theme.KuzmixSurfaceElevated
import com.kuzmix.ai.ui.theme.KuzmixTextPrimary
import com.kuzmix.ai.ui.theme.KuzmixTextSecondary

@Composable
fun KuzmixTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    minLines: Int = 1,
    maxLines: Int = 1
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .background(KuzmixSurfaceElevated, RoundedCornerShape(KuzmixRadius.Medium))
            .border(1.dp, KuzmixBorder, RoundedCornerShape(KuzmixRadius.Medium)),
        placeholder = {
            if (placeholder.isNotEmpty()) {
                Text(placeholder, color = KuzmixTextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        },
        minLines = minLines,
        maxLines = maxLines,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = KuzmixSurfaceElevated,
            unfocusedContainerColor = KuzmixSurfaceElevated,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedTextColor = KuzmixTextPrimary,
            unfocusedTextColor = KuzmixTextPrimary,
            cursorColor = com.kuzmix.ai.ui.theme.KuzmixOrange
        ),
        shape = RoundedCornerShape(KuzmixRadius.Medium),
        textStyle = MaterialTheme.typography.bodyLarge
    )
}

@Composable
fun KuzmixSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search anything..."
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(KuzmixSurfaceElevated, RoundedCornerShape(KuzmixRadius.Medium))
            .border(1.dp, KuzmixBorder, RoundedCornerShape(KuzmixRadius.Medium))
            .padding(horizontal = KuzmixSpacing.Lg, vertical = KuzmixSpacing.Md)
    ) {
        Text(
            text = value.ifEmpty { placeholder },
            color = if (value.isEmpty()) KuzmixTextSecondary else KuzmixTextPrimary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
