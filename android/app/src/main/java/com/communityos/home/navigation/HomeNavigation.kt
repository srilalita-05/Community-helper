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

import com.communityos.visitors.VisitorDetailsScreen
import com.communityos.visitors.ui.CreateVisitorScreen
import com.communityos.visitors.ui.ResidentVisitorsListScreen
import com.communityos.marketplace.ui.CreateEditListingScreen
import com.communityos.marketplace.ui.ListingDetailScreen
import com.communityos.marketplace.ui.MarketplaceListScreen

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
            },
            onNavigateToVisitors = {
                navController.navigate(Screen.VisitorsList.route)
            },
            onNavigateToCreateListing = {
                navController.navigate(Screen.CreateListing.route)
            },
            onNavigateToListingDetails = { listingId ->
                navController.navigate(Screen.ListingDetails.createRoute(listingId))
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

    // Resident Visitors Flow
    composable(Screen.VisitorsList.route) {
        ResidentVisitorsListScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToCreate = { navController.navigate(Screen.CreateVisitor.route) },
            onVisitorClick = { visitorId ->
                navController.navigate(Screen.VisitorDetails.createRoute(visitorId))
            }
        )
    }

    composable(Screen.CreateVisitor.route) {
        CreateVisitorScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(
        route = Screen.VisitorDetails.route,
        arguments = listOf(navArgument("visitorId") { type = NavType.StringType })
    ) {
        VisitorDetailsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    // Resident Marketplace Flow
    composable(Screen.MarketplaceList.route) {
        MarketplaceListScreen(
            onNavigateToCreateListing = { navController.navigate(Screen.CreateListing.route) },
            onNavigateToListingDetails = { listingId ->
                navController.navigate(Screen.ListingDetails.createRoute(listingId))
            }
        )
    }

    composable(
        route = Screen.ListingDetails.route,
        arguments = listOf(navArgument("listingId") { type = NavType.StringType })
    ) {
        ListingDetailScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToEditListing = { listingId ->
                navController.navigate(Screen.EditListing.createRoute(listingId))
            }
        )
    }

    composable(Screen.CreateListing.route) {
        CreateEditListingScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(
        route = Screen.EditListing.route,
        arguments = listOf(navArgument("listingId") { type = NavType.StringType })
    ) {
        CreateEditListingScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
