package com.hireflow.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.ViewKanban
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hireflow.app.HireFlowViewModel
import com.hireflow.app.ui.screens.LoginScreen
import com.hireflow.app.ui.screens.CandidateDetailScreen
import com.hireflow.app.ui.screens.CandidatesScreen
import com.hireflow.app.ui.screens.DashboardScreen
import com.hireflow.app.ui.screens.InterviewsScreen
import com.hireflow.app.ui.screens.PipelineScreen
import com.hireflow.app.ui.screens.ProfileScreen
import com.hireflow.app.ui.screens.ScorecardScreen
import com.hireflow.app.ui.theme.HireFlowTheme

private data class BottomDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomDestinations = listOf(
    BottomDestination("dashboard", "Tổng quan", Icons.Rounded.Dashboard),
    BottomDestination("candidates", "Ứng viên", Icons.Rounded.Groups),
    BottomDestination("pipeline", "Pipeline", Icons.Rounded.ViewKanban),
    BottomDestination("interviews", "Lịch", Icons.Rounded.CalendarMonth),
    BottomDestination("scorecards", "Đánh giá", Icons.Rounded.RateReview)
)

@Composable
fun HireFlowApp(viewModel: HireFlowViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val account by viewModel.accountState.collectAsStateWithLifecycle()
    HireFlowTheme(darkTheme = state.darkMode) {
        if (!account.authenticated && !account.offlineMode) {
            LoginScreen(account, viewModel::signIn, viewModel::signUp, viewModel::useOfflineDemo)
            return@HireFlowTheme
        }
        val navController = rememberNavController()
        val entry by navController.currentBackStackEntryAsState()
        val currentRoute = entry?.destination?.route.orEmpty()
        val showBottomBar = bottomDestinations.any { currentRoute == it.route }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showBottomBar) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    bottomDestinations.forEach { destination ->
                        val selected = currentRoute == destination.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, destination.label) },
                            label = { Text(destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("dashboard") {
                        DashboardScreen(
                            state = state,
                            onToggleTask = viewModel::toggleTask,
                            onToggleTheme = viewModel::setDarkMode,
                            account = account,
                            onSync = viewModel::syncNow,
                            onOpenProfile = { navController.navigate("profile") },
                            onOpenInterviews = { navController.navigate("interviews") },
                            onOpenCandidates = { navController.navigate("candidates") }
                        )
                    }
                    composable("candidates") {
                        CandidatesScreen(
                            candidates = state.candidates,
                            onAddCandidate = viewModel::addCandidate,
                            canManage = account.canManageRecruitment,
                            onOpenCandidate = { navController.navigate("candidate/$it") }
                        )
                    }
                    composable("pipeline") {
                        PipelineScreen(
                            candidates = state.candidates,
                            onOpenCandidate = { navController.navigate("candidate/$it") },
                            onMoveNext = viewModel::moveNext,
                            canManage = account.canManageRecruitment
                        )
                    }
                    composable("interviews") {
                        InterviewsScreen(
                            candidates = state.candidates,
                            interviews = state.interviews,
                            onAddInterview = viewModel::addInterview,
                            notificationsEnabled = state.notificationsEnabled,
                            canSchedule = account.canManageRecruitment,
                            onOpenCandidate = { navController.navigate("candidate/$it") },
                            onReview = { navController.navigate("scorecard/$it") }
                        )
                    }
                    composable("scorecards") {
                        ScorecardScreen(
                            candidates = state.candidates,
                            scorecards = state.scorecards,
                            initialCandidateId = null,
                            onSave = viewModel::saveScorecard,
                            onMoveNext = viewModel::moveNext,
                            onBack = null
                        )
                    }
                    composable("profile") {
                        ProfileScreen(
                            account = account,
                            state = state,
                            onBack = navController::navigateUp,
                            onToggleTheme = viewModel::setDarkMode,
                            onToggleNotifications = viewModel::setNotificationsEnabled,
                            onUpdateProfile = viewModel::updateProfile,
                            onSignOut = viewModel::signOut
                        )
                    }
                    composable("candidate/{id}") { backStack ->
                        val id = backStack.arguments?.getString("id")?.toLongOrNull()
                        CandidateDetailScreen(
                            candidate = state.candidates.firstOrNull { it.id == id },
                            scorecard = state.scorecards.firstOrNull { it.candidateId == id },
                            onBack = navController::navigateUp,
                            onAttachCv = viewModel::attachCv,
                            onUpdate = viewModel::updateCandidate,
                            canManage = account.canManageRecruitment,
                            onMoveNext = viewModel::moveNext,
                            onReject = viewModel::reject,
                            onReview = { navController.navigate("scorecard/$it") }
                        )
                    }
                    composable("scorecard/{id}") { backStack ->
                        val id = backStack.arguments?.getString("id")?.toLongOrNull()
                        ScorecardScreen(
                            candidates = state.candidates,
                            scorecards = state.scorecards,
                            initialCandidateId = id,
                            onSave = viewModel::saveScorecard,
                            onMoveNext = viewModel::moveNext,
                            onBack = navController::navigateUp
                        )
                    }
                }
            }
        }
    }
}
