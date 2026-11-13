package com.kuzmix.ai

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kuzmix.ai.di.AppContainer
import com.kuzmix.ai.ui.navigation.KuzmixNavGraph
import com.kuzmix.ai.ui.KuzmixViewModelFactory
import com.kuzmix.ai.ui.theme.KuzmixBackground
import com.kuzmix.ai.ui.theme.KuzmixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as KuzmixApp).container
        setContent {
            KuzmixAppContent(container)
        }
    }
}

@Composable
private fun KuzmixAppContent(container: AppContainer) {
    val appearance by container.settingsRepository.appearance.collectAsState(initial = "Dark")
    val systemDark = isSystemInDarkTheme()
    val dark = when (appearance) {
        "Light" -> false
        "Dark" -> true
        else -> systemDark
    }

    // System bar icons/tints follow the APP theme choice, not just the OS.
    // enableEdgeToEdge is a ComponentActivity extension — re-apply it from
    // the activity receiver, not the composable scope.
    val activity = LocalContext.current as? ComponentActivity
    LaunchedEffect(dark, activity) {
        val act = activity ?: return@LaunchedEffect
        val transparent = Color.TRANSPARENT
        if (dark) {
            act.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(transparent),
                navigationBarStyle = SystemBarStyle.dark(transparent)
            )
        } else {
            act.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.light(transparent, transparent),
                navigationBarStyle = SystemBarStyle.light(transparent, transparent)
            )
        }
    }

    KuzmixTheme(darkTheme = dark) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = KuzmixBackground
        ) {
            val factory = KuzmixViewModelFactory(container)
            val sharedViewModel: com.kuzmix.ai.ui.MainViewModel = viewModel(factory = factory)
            KuzmixNavGraph(
                container = container,
                factory = factory,
                mainViewModel = sharedViewModel
            )
        }
    }
}
