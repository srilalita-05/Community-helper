package com.communityos.home.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.communityos.complaints.ui.ComplaintDetailsScreen
import com.communityos.complaints.ui.ComplaintsListScreen
import com.communityos.complaints.ui.CreateComplaintScreen
import com.communityos.home.event.HomeEffect
import com.communityos.home.ui.DashboardScreen
import com.communityos.home.viewmodel.HomeViewModel
import com.communityos.navigation.Screen
import com.communityos.notices.ui.NoticeDetailsScreen
import com.communityos.notices.ui.NoticesListScreen

fun NavGraphBuilder.homeGraph(
    navController: NavHostController,
    onLogout: () -> Unit
) {
    composable(Screen.ResidentDashboard.route) {
        val viewModel: HomeViewModel = hiltViewModel()
        val state by viewModel.state.collectAsState()

        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    HomeEffect.NavigateToOnboarding -> onLogout()
                }
            }
        }

        DashboardScreen(
            state = state,
            onEvent = viewModel::onEvent,
            onNavigateToNotices = {
                navController.navigate(Screen.NoticesList.route)
            },
            onNavigateToComplaints = {
                navController.navigate(Screen.ComplaintsList.route)
            }
        )
    }

    composable(Screen.NoticesList.route) {
        NoticesListScreen(
            onNavigateBack = { navController.popBackStack() },
            onNoticeClick = { noticeId ->
                navController.navigate(Screen.NoticeDetails.createRoute(noticeId))
            }
        )
    }

    composable(
        route = Screen.NoticeDetails.route,
        arguments = listOf(navArgument("noticeId") { type = NavType.StringType })
    ) {
        NoticeDetailsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(Screen.ComplaintsList.route) {
        ComplaintsListScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToCreate = { navController.navigate(Screen.CreateComplaint.route) },
            onComplaintClick = { complaintId ->
                navController.navigate(Screen.ComplaintDetails.createRoute(complaintId))
            }
        )
    }

    composable(Screen.CreateComplaint.route) {
        CreateComplaintScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(
        route = Screen.ComplaintDetails.route,
        arguments = listOf(navArgument("complaintId") { type = NavType.StringType })
    ) {
        ComplaintDetailsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
