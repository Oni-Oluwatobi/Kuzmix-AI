package com.kuzmix.ai.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuzmix.ai.ui.theme.KuzmixBackground
import com.kuzmix.ai.ui.theme.KuzmixBorder
import com.kuzmix.ai.ui.theme.KuzmixOrange
import com.kuzmix.ai.ui.theme.KuzmixOrangeContainer
import com.kuzmix.ai.ui.theme.KuzmixRadius
import com.kuzmix.ai.ui.theme.KuzmixSpacing
import com.kuzmix.ai.ui.theme.KuzmixSurface
import com.kuzmix.ai.ui.theme.KuzmixTextOnAccent
import com.kuzmix.ai.ui.theme.KuzmixTextPrimary
import com.kuzmix.ai.ui.theme.KuzmixTextSecondary
import com.kuzmix.ai.ui.theme.KuzmixTextTertiary

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogout: () -> Unit
) {
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val modelStatus by viewModel.modelStatus.collectAsStateWithLifecycle()
    val isModelInstalled by viewModel.isModelInstalled.collectAsStateWithLifecycle()
    val modelFileLabel by viewModel.modelFileLabel.collectAsStateWithLifecycle()

    var activeDialog by remember { mutableStateOf("") }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importModel(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KuzmixBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KuzmixSpacing.ScreenHorizontal)
            .padding(top = KuzmixSpacing.Xxxl, bottom = KuzmixSpacing.Huge)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineMedium,
                color = KuzmixTextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(KuzmixSpacing.Xl))

        // Profile header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(KuzmixOrangeContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initialsOf(userName, userEmail),
                    style = MaterialTheme.typography.headlineMedium,
                    color = KuzmixOrange,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(KuzmixSpacing.Lg))

            Column {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleLarge,
                    color = KuzmixTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(KuzmixOrange)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Free Plan",
                            style = MaterialTheme.typography.labelSmall,
                            color = KuzmixTextOnAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = KuzmixTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(KuzmixSpacing.Xl))

        // Stats row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(KuzmixRadius.Card))
                .background(KuzmixSurface)
                .padding(vertical = KuzmixSpacing.Lg),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(label = "Projects", value = "12")
            StatDivider()
            StatItem(label = "Chats", value = "48")
            StatDivider()
            StatItem(label = "Tools Used", value = "15")
        }

        Spacer(modifier = Modifier.height(KuzmixSpacing.Xl))

        // Local engine status card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(KuzmixRadius.Card))
                .background(KuzmixSurface)
                .padding(KuzmixSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(KuzmixRadius.Small))
                    .background(KuzmixOrangeContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.SmartToy,
                    contentDescription = null,
                    tint = KuzmixOrange,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(KuzmixSpacing.Lg))
            Column {
                Text(
                    text = "Offline Engine",
                    style = MaterialTheme.typography.titleSmall,
                    color = KuzmixTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "DB v9 · 3,036 concepts · RAM cache · Offline core",
                    style = MaterialTheme.typography.bodySmall,
                    color = KuzmixTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(KuzmixSpacing.Xl))

        // Settings list
        SettingsRow(Icons.Outlined.Person, "Account", null) { activeDialog = "account" }
        SettingsRow(Icons.Outlined.Palette, "Appearance", appearance) { activeDialog = "appearance" }
        SettingsRow(Icons.Outlined.Language, "Language", language) { activeDialog = "language" }
        SettingsRow(Icons.Filled.Notifications, "Notifications", if (notificationsEnabled) "On" else "Off") {
            viewModel.setNotificationsEnabled(!notificationsEnabled)
        }
        SettingsRow(Icons.Outlined.Lock, "Privacy & Security", null) { activeDialog = "privacy" }
        SettingsRow(Icons.Outlined.Storage, "Data & Storage", null) { activeDialog = "data" }
        SettingsRow(Icons.Outlined.SmartToy, "AI Model", if (isModelInstalled) "Installed" else "Not installed") {
            activeDialog = "model"
        }
        SettingsRow(Icons.Filled.Info, "About Kuzmix AI", "v2.0.0") { activeDialog = "about" }

        Spacer(modifier = Modifier.height(KuzmixSpacing.Xl))

        // Log out
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(KuzmixRadius.Card))
                .border(1.5.dp, KuzmixOrange, RoundedCornerShape(KuzmixRadius.Card))
                .clickable(onClick = onLogout)
                .padding(vertical = KuzmixSpacing.Lg),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Logout,
                contentDescription = null,
                tint = KuzmixOrange,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(KuzmixSpacing.Sm))
            Text(
                text = "Log Out",
                style = MaterialTheme.typography.titleSmall,
                color = KuzmixOrange,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(KuzmixSpacing.Xl))

        Text(
            text = "Kuzmix AI by The Kreadive Galaxy",
            style = MaterialTheme.typography.labelSmall,
            color = KuzmixTextTertiary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }

    if (activeDialog.isNotEmpty()) {
        ProfileInfoDialog(
            type = activeDialog,
            userName = userName,
            userEmail = userEmail,
            appearance = appearance,
            language = language,
            modelStatus = modelStatus,
            engineName = viewModel.engineName,
            isModelInstalled = isModelInstalled,
            modelFileLabel = modelFileLabel,
            onAppearanceSelected = { viewModel.setAppearance(it) },
            onImportModel = { importLauncher.launch(arrayOf("*/*")) },
            onRemoveModel = { viewModel.removeModel() },
            onDismiss = { activeDialog = "" }
        )
    }
}

private fun initialsOf(name: String, email: String): String {
    val fromName = name.trim().split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
    if (fromName.isNotEmpty()) return fromName
    val local = email.substringBefore('@').trim()
    return local.take(2).uppercase()
}

@Composable
private fun ProfileInfoDialog(
    type: String,
    userName: String,
    userEmail: String,
    appearance: String,
    language: String,
    modelStatus: String,
    engineName: String,
    isModelInstalled: Boolean,
    modelFileLabel: String?,
    onAppearanceSelected: (String) -> Unit,
    onImportModel: () -> Unit,
    onRemoveModel: () -> Unit,
    onDismiss: () -> Unit
) {
    val title: String
    val body: @Composable () -> Unit = when (type) {
        "account" -> {
            title = "Account"
            {
                Text(
                    text = "$userName\n$userEmail",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KuzmixTextPrimary
                )
                Spacer(modifier = Modifier.height(KuzmixSpacing.Md))
                Text(
                    text = "Your class, term, and name sync to your Firebase account so they follow you across devices. Chats and voice recordings stay on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = KuzmixTextSecondary
                )
            }
        }
        "language" -> {
            title = "Language"
            {
                Text(
                    text = "Kuzmix AI follows your device language.\n\nCurrent language: $language",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KuzmixTextPrimary
                )
            }
        }
        "privacy" -> {
            title = "Privacy & Security"
            {
                Text(
                    text = "Kuzmix AI's study engine runs offline. Logging in and account creation need an internet connection; after that your class and term sync to Firebase. Chats, voice recordings, and personal data never leave your device, and questions are answered on-device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KuzmixTextPrimary
                )
            }
        }
        "data" -> {
            title = "Data & Storage"
            {
                Text(
                    text = "Conversations are stored locally in the app's private database. The onboard knowledge base (~114 MB) ships with the app. Clearing app data in Android settings removes all chats.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KuzmixTextPrimary
                )
            }
        }
        "model" -> {
            title = "AI Model"
            {
                Text(
                    text = "Engine: $engineName\nStatus: $modelStatus",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KuzmixTextPrimary
                )
                if (modelFileLabel != null) {
                    Spacer(modifier = Modifier.height(KuzmixSpacing.Sm))
                    Text(
                        text = "File: $modelFileLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = KuzmixTextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(KuzmixSpacing.Md))
                Text(
                    text = "Import a Gemma .task bundle or a Qwen/Llama .gguf " +
                        "(1–3 GB) once — after that, ChatGPT-style answers run " +
                        "completely offline, grounded in the onboard knowledge " +
                        "base. Without a model, answers come from the offline " +
                        "knowledge engine.",
                    style = MaterialTheme.typography.bodySmall,
                    color = KuzmixTextSecondary
                )
                Spacer(modifier = Modifier.height(KuzmixSpacing.Md))
                TextButton(onClick = onImportModel) {
                    Text(
                        text = if (isModelInstalled) "Import another model" else "Import model file",
                        color = KuzmixOrange
                    )
                }
                if (isModelInstalled) {
                    TextButton(onClick = onRemoveModel) {
                        Text(text = "Remove model", color = KuzmixOrange)
                    }
                }
            }
        }
        "about" -> {
            title = "About Kuzmix AI"
            {
                Text(
                    text = "Kuzmix AI v2.0.0\nby The Kreadive Galaxy\n\nOffline AI assistant with a 3,036-concept onboard knowledge base. Internet is used only to log in and sync your class.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KuzmixTextPrimary
                )
            }
        }
        else -> {
            title = "Appearance"
            {
                listOf("Dark", "Light", "System").forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = appearance == option,
                            onClick = { onAppearanceSelected(option) }
                        )
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium,
                            color = KuzmixTextPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(KuzmixSpacing.Sm))
                Text(
                    text = "Your choice applies immediately and is saved for future launches.",
                    style = MaterialTheme.typography.bodySmall,
                    color = KuzmixTextSecondary
                )
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KuzmixSurface,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = KuzmixTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = body,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = KuzmixOrange)
            }
        }
    )
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = KuzmixTextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = KuzmixTextSecondary
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .height(32.dp)
            .width(1.dp)
            .background(KuzmixBorder)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    value: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = KuzmixSpacing.Lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = KuzmixTextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(KuzmixSpacing.Lg))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = KuzmixTextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = KuzmixTextSecondary
            )
            Spacer(modifier = Modifier.width(KuzmixSpacing.Sm))
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = KuzmixTextTertiary
        )
    }
}
