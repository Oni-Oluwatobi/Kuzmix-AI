package com.kuzmix.ai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kuzmix.ai.ui.theme.KuzmixBorder
import com.kuzmix.ai.ui.theme.KuzmixOrange
import com.kuzmix.ai.ui.theme.KuzmixOrangeDeep
import com.kuzmix.ai.ui.theme.KuzmixRadius
import com.kuzmix.ai.ui.theme.KuzmixSpacing
import com.kuzmix.ai.ui.theme.KuzmixTextOnAccent
import com.kuzmix.ai.ui.theme.KuzmixTextPrimary

@Composable
fun KuzmixButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    style: KuzmixButtonStyle = KuzmixButtonStyle.Primary
) {
    val containerColor = when (style) {
        KuzmixButtonStyle.Primary -> KuzmixOrange
        KuzmixButtonStyle.Secondary -> KuzmixTextPrimary
        KuzmixButtonStyle.Outline -> androidx.compose.ui.graphics.Color.Transparent
    }
    val contentColor = when (style) {
        KuzmixButtonStyle.Primary -> KuzmixTextOnAccent
        KuzmixButtonStyle.Secondary -> KuzmixTextOnAccent
        KuzmixButtonStyle.Outline -> KuzmixOrangeDeep
    }

    val buttonModifier = modifier
        .fillMaxWidth()
        .height(52.dp)
        .sizeIn(minHeight = 48.dp)

    if (style == KuzmixButtonStyle.Outline) {
        OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled && !loading,
            shape = RoundedCornerShape(KuzmixRadius.Medium),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, KuzmixOrange),
            contentPadding = PaddingValues(horizontal = KuzmixSpacing.Xl, vertical = KuzmixSpacing.Md)
        ) {
            ButtonContent(text, loading, leadingIcon, contentColor)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled && !loading,
            shape = RoundedCornerShape(KuzmixRadius.Medium),
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = containerColor.copy(alpha = 0.4f)
            ),
            contentPadding = PaddingValues(horizontal = KuzmixSpacing.Xl, vertical = KuzmixSpacing.Md)
        ) {
            ButtonContent(text, loading, leadingIcon, contentColor)
        }
    }
}

@Composable
private fun ButtonContent(
    text: String,
    loading: Boolean,
    leadingIcon: ImageVector?,
    contentColor: androidx.compose.ui.graphics.Color
) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = contentColor,
            strokeWidth = 2.dp
        )
    } else {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            }
            Text(
                text = text,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

@Composable
fun KuzmixTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text = text, color = KuzmixOrange, fontWeight = FontWeight.SemiBold)
    }
}

enum class KuzmixButtonStyle { Primary, Secondary, Outline }
