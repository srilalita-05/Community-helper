package com.communityos.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.communityos.authentication.navigation.authGraph
import com.communityos.home.navigation.homeGraph
import com.communityos.admin.AdminDashboardScreen
import com.communityos.clubs.ClubDetailsScreen
import com.communityos.security.SecurityDashboardScreen
import com.communityos.security.ui.SecurityVisitorDetailsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // Delegate to Auth modular navigation graph
        authGraph(
            navController = navController,
            onAuthComplete = { role ->
                val destination = when (role) {
                    "Admin" -> Screen.AdminDashboard.route
                    "Security" -> Screen.SecurityDashboard.route
                    else -> Screen.ResidentDashboard.route
                }
                navController.navigate(destination) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
        )

        // Delegate to Home modular navigation graph
        homeGraph(
            navController = navController,
            onLogout = {
                navController.navigate(Screen.Onboarding.route) {
                    popUpTo(Screen.ResidentDashboard.route) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        )

        // Other Dashboards
        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen()
        }
        composable(Screen.SecurityDashboard.route) {
            SecurityDashboardScreen(
                onLogout = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.SecurityDashboard.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onVisitorClick = { visitorId ->
                    navController.navigate(Screen.SecurityVisitorDetails.createRoute(visitorId))
                }
            )
        }

        // Security Visitor Details
        composable(
            route = Screen.SecurityVisitorDetails.route,
            arguments = listOf(navArgument("visitorId") { type = NavType.StringType })
        ) {
            SecurityVisitorDetailsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ClubDetails.route,
            arguments = listOf(navArgument("clubId") { type = NavType.StringType })
        ) { backStackEntry ->
            val clubId = backStackEntry.arguments?.getString("clubId") ?: ""
            ClubDetailsScreen(clubId = clubId)
        }
    }
}
