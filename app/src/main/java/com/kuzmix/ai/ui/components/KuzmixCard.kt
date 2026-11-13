package com.kuzmix.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.kuzmix.ai.ui.theme.KuzmixBorderSubtle
import com.kuzmix.ai.ui.theme.KuzmixRadius
import com.kuzmix.ai.ui.theme.KuzmixSpacing
import com.kuzmix.ai.ui.theme.KuzmixSurface

@Composable
fun KuzmixCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showBorder: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(KuzmixRadius.Card)
    val base = modifier
        .clip(shape)
        .background(KuzmixSurface)
        .then(
            if (showBorder) Modifier.border(1.dp, KuzmixBorderSubtle, shape)
            else Modifier
        )
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(KuzmixSpacing.Lg)

    Column(modifier = base, content = content)
}

@Composable
fun KuzmixSurfaceBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(KuzmixRadius.Card))
            .background(KuzmixSurface)
    ) {
        content()
    }
}
