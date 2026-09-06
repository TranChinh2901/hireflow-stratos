package com.hireflow.app.ui

import androidx.compose.runtime.CompositionLocalProvider
import com.hireflow.app.ui.components.LocalHeaderNavigation
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.hireflow.app.HireFlowViewModel
import com.hireflow.app.reminder.syncInterviewReminders
import com.hireflow.app.ui.screens.LoginScreen
import com.hireflow.app.ui.screens.CandidateDetailScreen
import com.hireflow.app.ui.screens.CandidatesScreen
import com.hireflow.app.ui.screens.DashboardScreen
import com.hireflow.app.ui.screens.InterviewsScreen
import com.hireflow.app.ui.screens.InterviewDetailScreen
import com.hireflow.app.ui.screens.PipelineScreen
import com.hireflow.app.ui.screens.ProfileScreen
import com.hireflow.app.ui.screens.ScorecardScreen
import com.hireflow.app.ui.theme.HireFlowTheme

private data class BottomDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private fun scorecardRoute(candidateId: Long, interviewId: Long?, locked: Boolean): String {
    val params = listOfNotNull(
        interviewId?.let { "interviewId=$it" },
        if (locked) "locked=true" else null
    ).joinToString("&")
    return if (params.isEmpty()) "scorecard/$candidateId" else "scorecard/$candidateId?$params"
}

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
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    HireFlowTheme(darkTheme = state.darkMode) {
        if (!account.authenticated && !account.offlineMode) {
            LoginScreen(account, viewModel::signIn, viewModel::signUp, viewModel::useOfflineDemo)
            return@HireFlowTheme
        }
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }
        val context = LocalContext.current
        LaunchedEffect(notice) {
            notice?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearNotice()
            }
        }
        LaunchedEffect(state.interviews, state.notificationsEnabled) {
            syncInterviewReminders(context, state.interviews, state.notificationsEnabled)
        }
        val entry by navController.currentBackStackEntryAsState()
        val currentRoute = entry?.destination?.route.orEmpty()
        val currentBase = currentRoute.substringBefore("?").substringBefore("/{")
        val showBottomBar = bottomDestinations.any { currentRoute == it.route || currentBase == it.route }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showBottomBar) NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    bottomDestinations.forEach { destination ->
                        val selected = currentBase == destination.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, destination.label, modifier = Modifier.size(21.dp)) },
                            label = { Text(destination.label, style = MaterialTheme.typography.labelMedium, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { padding ->
            CompositionLocalProvider(LocalHeaderNavigation provides { route ->
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }) {
                Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
                    NavHost(navController = navController, startDestination = "dashboard") {
                        composable("dashboard") {
                            DashboardScreen(
                                state = state,
                                onToggleTheme = viewModel::setDarkMode,
                                account = account,
                                onSync = viewModel::syncNow,
                                onOpenProfile = { navController.navigate("profile") },
                                onOpenInterviews = { navController.navigate("interviews") },
                                onOpenCandidate = { navController.navigate("candidate/$it") },
                                onOpenInterview = { navController.navigate("interview/$it") },
                                onOpenReview = { candidateId, interviewId ->
                                    navController.navigate(scorecardRoute(candidateId, interviewId, locked = true))
                                },
                                onOpenCandidatesFiltered = { stage, need ->
                                    val params = listOfNotNull(
                                        stage?.let { "stage=$it" },
                                        need?.let { "need=$it" }
                                    ).joinToString("&")
                                    val route = if (params.isEmpty()) "candidates" else "candidates?$params"
                                    // Entry mới mỗi lần bấm để args lọc luôn được áp dụng,
                                    // không bị restoreState giữ ngữ cảnh cũ.
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        composable(
                            "candidates?stage={stage}&need={need}",
                            arguments = listOf(
                                navArgument("stage") { type = NavType.StringType; nullable = true; defaultValue = null },
                                navArgument("need") { type = NavType.StringType; nullable = true; defaultValue = null }
                            )
                        ) { backStack ->
                            CandidatesScreen(
                                candidates = state.candidates,
                                interviews = state.interviews,
                                scorecards = state.scorecards,
                                onAddCandidate = viewModel::addCandidate,
                                canManage = account.canManageRecruitment,
                                onOpenCandidate = { navController.navigate("candidate/$it") },
                                onDeleteRejected = viewModel::deleteRejectedCandidates,
                                onDeleteCandidate = viewModel::deleteCandidate,
                                initialStage = backStack.arguments?.getString("stage"),
                                initialNeed = backStack.arguments?.getString("need")
                            )
                        }
                        composable("pipeline") {
                            PipelineScreen(
                                candidates = state.candidates,
                                interviews = state.interviews,
                                scorecards = state.scorecards,
                                onOpenCandidate = { navController.navigate("candidate/$it") },
                                onMoveNext = viewModel::moveNext,
                                canManage = account.canManageRecruitment
                            )
                        }
                        composable(
                            "interviews?candidateId={candidateId}&openDialog={openDialog}",
                            arguments = listOf(
                                navArgument("candidateId") { type = NavType.LongType; defaultValue = -1L },
                                navArgument("openDialog") { type = NavType.BoolType; defaultValue = false }
                            )
                        ) { backStack ->
                            val candidateArg = backStack.arguments?.getLong("candidateId")?.takeIf { it != -1L }
                            InterviewsScreen(
                                candidates = state.candidates,
                                interviews = state.interviews,
                                onAddInterview = viewModel::addInterview,
                                notificationsEnabled = state.notificationsEnabled,
                                canSchedule = account.canManageRecruitment,
                                currentInterviewer = account.profile?.fullName ?: "Linh HR",
                                initialCandidateId = candidateArg,
                                openDialog = backStack.arguments?.getBoolean("openDialog") == true,
                                onOpenInterview = { navController.navigate("interview/$it") },
                                onReview = { candidateId, interviewId ->
                                    navController.navigate(scorecardRoute(candidateId, interviewId, locked = true))
                                }
                            )
                        }
                        composable(
                            "interview/{interviewId}",
                            arguments = listOf(navArgument("interviewId") { type = NavType.LongType })
                        ) { backStack ->
                            val interviewId = backStack.arguments?.getLong("interviewId")
                            val interview = state.interviews.firstOrNull { it.id == interviewId }
                            InterviewDetailScreen(
                                interview = interview,
                                candidate = state.candidates.firstOrNull { it.id == interview?.candidateId },
                                canSchedule = account.canManageRecruitment,
                                notificationsEnabled = state.notificationsEnabled,
                                onBack = navController::navigateUp,
                                onOpenCandidate = { navController.navigate("candidate/$it") },
                                onReview = { candidateId, interviewIdArg ->
                                    navController.navigate(scorecardRoute(candidateId, interviewIdArg, locked = true))
                                },
                                onSetCompleted = viewModel::setInterviewCompleted,
                                onReschedule = viewModel::rescheduleInterview,
                                onCancel = viewModel::cancelInterview,
                                onNoShow = viewModel::markInterviewNoShow
                            )
                        }
                        composable("scorecards") {
                            ScorecardScreen(
                                candidates = state.candidates,
                                interviews = state.interviews,
                                scorecards = state.scorecards,
                                evaluatorId = account.profile?.id,
                                initialCandidateId = null,
                                lockedCandidate = false,
                                interviewId = null,
                                onSave = viewModel::saveScorecard,
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
                            val detailCandidate = state.candidates.firstOrNull { it.id == id }
                            CandidateDetailScreen(
                                candidate = detailCandidate,
                                scorecard = state.scorecards.firstOrNull {
                                    it.candidateId == id && it.evaluatorId == account.profile?.id
                                },
                                candidateScorecards = state.scorecards.filter { it.candidateId == id },
                                interviews = state.interviews,
                                histories = state.histories.filter { it.candidateId == id },
                                onBack = navController::navigateUp,
                                onAttachCv = viewModel::attachCv,
                                onOpenCv = viewModel::openCv,
                                onUpdate = viewModel::updateCandidate,
                                canManage = account.canManageRecruitment,
                                onMoveNext = viewModel::moveNext,
                                onReject = viewModel::reject,
                                onRecordResponse = viewModel::recordOfferResponse,
                                onReview = { candidateId, interviewId ->
                                    // Giữ ngữ cảnh buổi phỏng vấn: ưu tiên buổi được chỉ định,
                                    // nếu mở từ hồ sơ thì lấy buổi hoàn thành gần nhất.
                                    val contextId = interviewId
                                        ?: state.interviews
                                            .filter { it.candidateId == candidateId && it.completed }
                                            .maxByOrNull { it.scheduledAt }?.id
                                    navController.navigate(scorecardRoute(candidateId, contextId, locked = true))
                                },
                                onSchedule = { candidateId ->
                                    navController.navigate("interviews?candidateId=$candidateId&openDialog=true")
                                },
                                onOpenInterview = { navController.navigate("interview/$it") },
                                onSetCompleted = viewModel::setInterviewCompleted,
                                onDelete = viewModel::deleteCandidate
                            )
                        }
                        composable(
                            "scorecard/{candidateId}?interviewId={interviewId}&locked={locked}",
                            arguments = listOf(
                                navArgument("candidateId") { type = NavType.LongType },
                                navArgument("interviewId") { type = NavType.LongType; defaultValue = -1L },
                                navArgument("locked") { type = NavType.BoolType; defaultValue = false }
                            )
                        ) { backStack ->
                            val candidateArg = backStack.arguments?.getLong("candidateId")
                            val interviewArg = backStack.arguments?.getLong("interviewId")?.takeIf { it != -1L }
                            ScorecardScreen(
                                candidates = state.candidates,
                                interviews = state.interviews,
                                scorecards = state.scorecards,
                                evaluatorId = account.profile?.id,
                                initialCandidateId = candidateArg,
                                lockedCandidate = backStack.arguments?.getBoolean("locked") == true,
                                interviewId = interviewArg,
                                onSave = viewModel::saveScorecard,
                                onBack = navController::navigateUp
                            )
                        }
                    }
                }
            }
        }
    }
}
