package com.kuzmix.ai.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kuzmix.ai.di.AppContainer
import com.kuzmix.ai.ui.KuzmixViewModelFactory
import com.kuzmix.ai.ui.MainViewModel
import com.kuzmix.ai.ui.components.KuzmixBottomBar
import com.kuzmix.ai.ui.screens.chat.ChatScreen
import com.kuzmix.ai.ui.screens.chat.ChatViewModel
import com.kuzmix.ai.ui.screens.chats.ChatsScreen
import com.kuzmix.ai.ui.screens.chats.ChatsViewModel
import com.kuzmix.ai.ui.screens.code.CodeAssistantScreen
import com.kuzmix.ai.ui.screens.code.CodeViewModel
import com.kuzmix.ai.ui.screens.home.HomeScreen
import com.kuzmix.ai.ui.screens.home.HomeViewModel
import com.kuzmix.ai.ui.screens.imagegen.ImageGenScreen
import com.kuzmix.ai.ui.screens.imagegen.ImageGenViewModel
import com.kuzmix.ai.ui.screens.profile.ProfileScreen
import com.kuzmix.ai.ui.screens.profile.ProfileViewModel
import com.kuzmix.ai.ui.screens.tools.ToolsScreen
import com.kuzmix.ai.ui.screens.welcome.WelcomeScreen
import com.kuzmix.ai.ui.theme.KuzmixBackground

@Composable
fun KuzmixNavGraph(
    container: AppContainer,
    factory: KuzmixViewModelFactory,
    mainViewModel: MainViewModel
) {
    val navController = rememberNavController()
    val isFirstLaunch by mainViewModel.isFirstLaunchDone.collectAsStateWithLifecycle()
    val userName by mainViewModel.userName.collectAsStateWithLifecycle()
    val conversations by mainViewModel.conversations.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Firebase session (null = signed out). Seeded from the cached user so
    // returning users never see the login gate flash.
    val authUser by container.authRepository.authState.collectAsStateWithLifecycle()

    val bottomBarRoutes = setOf("home", "chats", "tools", "profile")
    val showBottomBar = currentRoute in bottomBarRoutes

    when (isFirstLaunch) {
        null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        false -> {
            WelcomeScreen(
                onGetStarted = {
                    mainViewModel.markFirstLaunchDone()
                }
            )
        }
        true -> {
            // Internet-first gate: no account, no app (v2.0 Firebase login).
            if (authUser == null) {
                val authVm: com.kuzmix.ai.ui.screens.auth.AuthViewModel = viewModel(factory = factory)
                com.kuzmix.ai.ui.screens.auth.AuthScreen(
                    viewModel = authVm,
                    googleSignInIntent = { container.authRepository.googleSignInIntent() }
                )
            } else {
                Scaffold(
                containerColor = KuzmixBackground,
                contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
                bottomBar = {
                    if (showBottomBar) {
                        KuzmixBottomBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                if (currentRoute != route) {
                                    navController.navigate(route) {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            onCreateClick = {
                                mainViewModel.createConversation { id ->
                                    navController.navigate(Screen.Chat.create(id))
                                }
                            }
                        )
                    }
                }
            ) { padding ->
                AppNavHost(
                    navController = navController,
                    container = container,
                    factory = factory,
                    mainViewModel = mainViewModel,
                    userName = userName,
                    conversations = conversations,
                    modifier = Modifier.padding(padding)
                )
            }
            }
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    container: AppContainer,
    factory: KuzmixViewModelFactory,
    mainViewModel: MainViewModel,
    userName: String,
    conversations: List<com.kuzmix.ai.domain.model.Conversation>,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") {
            val vm: HomeViewModel = viewModel(factory = factory)
            val homeConversations by vm.conversations.collectAsStateWithLifecycle()
            val knowledgeHits by vm.knowledgeHits.collectAsStateWithLifecycle()
            HomeScreen(
                userName = userName,
                conversations = homeConversations,
                offlineReady = true,
                knowledgeHits = knowledgeHits,
                onSearchQuery = { vm.onSearchQuery(it) },
                onAskKnowledge = { q ->
                    mainViewModel.setPendingInitialMessage(q)
                    mainViewModel.createConversation(title = q.take(60)) { id ->
                        navController.navigate(Screen.Chat.create(id))
                    }
                },
                onNewChat = {
                    vm.startNewChat { id ->
                        navController.navigate(Screen.Chat.create(id))
                    }
                },
                onOpenChat = { id ->
                    navController.navigate(Screen.Chat.create(id))
                },
                onOpenImageGen = { navController.navigate("image_gen") },
                onOpenCode = { navController.navigate("code_assistant") },
                onOpenTools = { navController.navigate("tools") },
                onSeeAllChats = { navController.navigate("chats") },
                onStudyPlan = {
                    mainViewModel.createConversation(title = "Study Plan") { id ->
                        navController.navigate(Screen.Chat.create(id))
                    }
                }
            )
        }

        composable("chats") {
            val vm: ChatsViewModel = viewModel(factory = factory)
            ChatsScreen(
                viewModel = vm,
                onOpenChat = { id -> navController.navigate(Screen.Chat.create(id)) },
                onNewChat = {
                    mainViewModel.createConversation { id ->
                        navController.navigate(Screen.Chat.create(id))
                    }
                }
            )
        }

        composable("tools") {
            ToolsScreen(
                onOpenImageGen = { navController.navigate("image_gen") },
                onOpenCode = { navController.navigate("code_assistant") },
                onOpenSummarizer = {
                    mainViewModel.createConversation(title = "Summarizer") { id ->
                        navController.navigate(Screen.Chat.create(id))
                    }
                }
            )
        }

        composable("profile") {
            val vm: ProfileViewModel = viewModel(factory = factory)
            ProfileScreen(
                viewModel = vm,
                // sign out -> authState flips null -> login gate reappears
                onLogout = { container.authRepository.signOut() }
            )
        }

        composable(
            route = "chat/{conversationId}",
            arguments = listOf(navArgument("conversationId") { type = NavType.LongType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getLong("conversationId") ?: return@composable
            val vm: ChatViewModel = viewModel(factory = factory)
            val pendingInitial = remember { mainViewModel.consumePendingInitialMessage() }
            ChatScreen(
                viewModel = vm,
                conversationId = conversationId,
                initialMessage = pendingInitial,
                onBack = { navController.popBackStack() }
            )
        }

        composable("image_gen") {
            val vm: ImageGenViewModel = viewModel(factory = factory)
            ImageGenScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable("code_assistant") {
            val vm: CodeViewModel = viewModel(factory = factory)
            CodeAssistantScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
