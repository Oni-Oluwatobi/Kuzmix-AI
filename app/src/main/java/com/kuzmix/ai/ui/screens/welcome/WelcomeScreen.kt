package com.kuzmix.ai.ui.screens.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.kuzmix.ai.R
import com.kuzmix.ai.ui.components.KuzmixButton
import com.kuzmix.ai.ui.components.KuzmixTextLink
import com.kuzmix.ai.ui.theme.KuzmixBackground
import com.kuzmix.ai.ui.theme.KuzmixOrange
import com.kuzmix.ai.ui.theme.KuzmixSpacing
import com.kuzmix.ai.ui.theme.KuzmixTextPrimary
import com.kuzmix.ai.ui.theme.KuzmixTextSecondary
import com.kuzmix.ai.ui.theme.KuzmixYellow

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KuzmixBackground)
            .padding(KuzmixSpacing.ScreenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Image(
            painter = painterResource(id = R.drawable.ic_kuzmix_logo),
            contentDescription = "Kuzmix AI Logo",
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(KuzmixSpacing.Xl))

        Text(
            text = "Kuzmix AI",
            style = MaterialTheme.typography.displayMedium,
            color = KuzmixTextPrimary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "by The Kreadive Galaxy",
            style = MaterialTheme.typography.bodyMedium,
            color = KuzmixTextSecondary
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = buildAnnotatedString {
                append("Smarter Conversations.\nBigger Ideas.\n")
                withStyle(SpanStyle(color = KuzmixOrange)) {
                    append("A Brighter You.")
                }
            },
            style = MaterialTheme.typography.headlineMedium,
            color = KuzmixTextPrimary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            lineHeight = MaterialTheme.typography.headlineMedium.lineHeight
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(KuzmixOrange)
        )

        Spacer(modifier = Modifier.height(KuzmixSpacing.Xxl))

        KuzmixButton(
            text = "Get Started",
            onClick = onGetStarted
        )

        Spacer(modifier = Modifier.height(KuzmixSpacing.Lg))

        KuzmixTextLink(
            text = "Already have an account?  Log In",
            onClick = onGetStarted
        )

        Spacer(modifier = Modifier.height(KuzmixSpacing.Xxl))

        Text(
            text = "Empowering the World\nwith Innovative AI",
            style = MaterialTheme.typography.bodySmall,
            color = KuzmixTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(KuzmixSpacing.Xl))
    }
}
