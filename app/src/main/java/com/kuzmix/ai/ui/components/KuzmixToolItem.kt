package com.kuzmix.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kuzmix.ai.ui.theme.KuzmixBorderSubtle
import com.kuzmix.ai.ui.theme.KuzmixOrange
import com.kuzmix.ai.ui.theme.KuzmixOrangeContainer
import com.kuzmix.ai.ui.theme.KuzmixSpacing
import com.kuzmix.ai.ui.theme.KuzmixSurface
import com.kuzmix.ai.ui.theme.KuzmixTextPrimary
import com.kuzmix.ai.ui.theme.KuzmixTextSecondary
import com.kuzmix.ai.ui.theme.KuzmixTextTertiary

@Composable
fun KuzmixToolItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingText: String? = null
) {
    Row(
        modifier = modifier
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
                imageVector = icon,
                contentDescription = null,
                tint = KuzmixOrange,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(KuzmixSpacing.Lg))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
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

        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodyMedium,
                color = KuzmixTextSecondary,
                modifier = Modifier.padding(end = KuzmixSpacing.Sm)
            )
        }

        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = KuzmixTextTertiary
        )
    }
}

@Composable
fun KuzmixSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = KuzmixTextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        if (actionText != null && onAction != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelLarge,
                color = KuzmixOrange,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

@Composable
fun KuzmixDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = KuzmixSpacing.Sm)
            .then(
                Modifier.background(
                    KuzmixBorderSubtle,
                    RoundedCornerShape(1.dp)
                )
            )
            .size(width = 0.dp, height = 1.dp)
    )
}
