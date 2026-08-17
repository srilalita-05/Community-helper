package com.communityos.home.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.communityos.home.ui.DashboardScreen
import com.communityos.home.viewmodel.HomeViewModel
import com.communityos.navigation.Screen

fun NavGraphBuilder.homeGraph(
    navController: NavHostController,
    onLogout: () -> Unit
) {
    composable(Screen.ResidentDashboard.route) {
        val viewModel: HomeViewModel = hiltViewModel()
        val state by viewModel.state.collectAsState()

        DashboardScreen(
            state = state,
            onEvent = viewModel::onEvent,
            onLogout = onLogout
        )
    }
}
