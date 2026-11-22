package com.kuzmix.ai.ui.screens.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuzmix.ai.R
import com.kuzmix.ai.data.auth.AuthRepository
import com.kuzmix.ai.ui.components.KuzmixButton
import com.kuzmix.ai.ui.components.KuzmixButtonStyle
import com.kuzmix.ai.ui.components.KuzmixTextField
import com.kuzmix.ai.ui.components.KuzmixTextLink
import com.kuzmix.ai.ui.theme.KuzmixBackground
import com.kuzmix.ai.ui.theme.KuzmixError
import com.kuzmix.ai.ui.theme.KuzmixOrange
import com.kuzmix.ai.ui.theme.KuzmixOrangeContainer
import com.kuzmix.ai.ui.theme.KuzmixRadius
import com.kuzmix.ai.ui.theme.KuzmixSpacing
import com.kuzmix.ai.ui.theme.KuzmixTextPrimary
import com.kuzmix.ai.ui.theme.KuzmixTextSecondary

/**
 * Internet-first gate: the student must connect and either log in or
 * create an account before the app opens (v2.0 Firebase login). Offline
 * shows a blocking notice with a retry — nothing here works without a
 * network by design.
 */
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    googleSignInIntent: () -> android.content.Intent?
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Re-check the connection whenever we land on / resume the gate.
    LaunchedEffect(Unit) { viewModel.refreshConnectivity() }

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.signInWithGoogle(result.data)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KuzmixBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KuzmixSpacing.ScreenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(KuzmixSpacing.Huge))

        Image(
            painter = painterResource(id = R.drawable.ic_kuzmix_logo),
            contentDescription = "Kuzmix AI Logo",
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(KuzmixSpacing.Lg))

        Text(
            text = if (state.mode == AuthMode.SIGN_IN) "Welcome back" else "Create your account",
            style = MaterialTheme.typography.headlineSmall,
            color = KuzmixTextPrimary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Connect to the internet to log in or sign up —\nyour class and progress follow your account.",
            style = MaterialTheme.typography.bodyMedium,
            color = KuzmixTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(KuzmixSpacing.Xl))

        // -------------------------------------------------- offline gate
        if (state.checkedOnline && !state.online) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(KuzmixRadius.Card))
                    .background(KuzmixOrangeContainer)
                    .padding(KuzmixSpacing.Lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📡 You're offline",
                    style = MaterialTheme.typography.titleSmall,
                    color = KuzmixTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(KuzmixSpacing.Sm))
                Text(
                    text = AuthRepository.OFFLINE_MESSAGE,
                    style = MaterialTheme.typography.bodyMedium,
                    color = KuzmixTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(KuzmixSpacing.Md))
                KuzmixButton(
                    text = "Check again",
                    onClick = { viewModel.refreshConnectivity() },
                    style = KuzmixButtonStyle.Outline
                )
            }
            Spacer(modifier = Modifier.height(KuzmixSpacing.Xl))
        }

        // ------------------------------------------------------- form
        if (state.mode == AuthMode.CREATE_ACCOUNT) {
            KuzmixTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                placeholder = "Full name"
            )
            Spacer(modifier = Modifier.height(KuzmixSpacing.Md))
        }

        KuzmixTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            placeholder = "Email address",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(KuzmixSpacing.Md))

        KuzmixTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            placeholder = if (state.mode == AuthMode.SIGN_IN) "Password" else "Password (min 6 characters)",
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            )
        )

        state.error?.let { message ->
            Spacer(modifier = Modifier.height(KuzmixSpacing.Md))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (message == AuthRepository.OFFLINE_MESSAGE) KuzmixOrange else KuzmixError,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(KuzmixSpacing.Xl))

        KuzmixButton(
            text = if (state.mode == AuthMode.SIGN_IN) "Log In" else "Create Account",
            onClick = viewModel::submit,
            enabled = state.online && !state.loading,
            loading = state.loading
        )

        if (state.googleAvailable) {
            Spacer(modifier = Modifier.height(KuzmixSpacing.Md))
            KuzmixButton(
                text = "Continue with Google",
                onClick = {
                    googleSignInIntent()?.let { intent -> googleLauncher.launch(intent) }
                },
                enabled = state.online && !state.loading,
                style = KuzmixButtonStyle.Outline
            )
        }

        Spacer(modifier = Modifier.height(KuzmixSpacing.Lg))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (state.mode == AuthMode.SIGN_IN)
                    "New to Kuzmix AI?" else "Already have an account?",
                style = MaterialTheme.typography.bodyMedium,
                color = KuzmixTextSecondary
            )
            KuzmixTextLink(
                text = if (state.mode == AuthMode.SIGN_IN) "  Create account" else "  Log in",
                onClick = {
                    viewModel.setMode(
                        if (state.mode == AuthMode.SIGN_IN) AuthMode.CREATE_ACCOUNT
                        else AuthMode.SIGN_IN
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(KuzmixSpacing.Xxl))
    }
}
