package com.communityos.authentication.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.communityos.authentication.ui.*
import com.communityos.authentication.viewmodel.AuthViewModel
import com.communityos.navigation.Screen

fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    onAuthComplete: (role: String) -> Unit
) {
    composable(Screen.Splash.route) {
        SplashScreen(
            onNavigateToHome = {
                navController.navigate(Screen.ResidentDashboard.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            },
            onNavigateToAuth = {
                navController.navigate(Screen.Onboarding.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        )
    }

    composable(Screen.Onboarding.route) {
        OnboardingScreen(
            onGetStarted = {
                navController.navigate(Screen.Login.route)
            }
        )
    }

    // Shared ViewModel scope for all auth flow screens to maintain State across steps
    composable(Screen.Login.route) { backStackEntry ->
        val parentEntry = remember(backStackEntry) {
            navController.getBackStackEntry(Screen.Login.route)
        }
        val viewModel: AuthViewModel = hiltViewModel(parentEntry)
        val state by viewModel.state.collectAsState()

        LoginScreen(
            state = state,
            onEvent = viewModel::onEvent,
            onNavigateToOtp = { phone ->
                navController.navigate(Screen.OtpVerification.createRoute(phone))
            }
        )
    }

    composable(
        route = Screen.OtpVerification.route,
        arguments = listOf(navArgument("phone") { type = NavType.StringType })
    ) { backStackEntry ->
        val parentEntry = remember(backStackEntry) {
            navController.getBackStackEntry(Screen.Login.route)
        }
        val viewModel: AuthViewModel = hiltViewModel(parentEntry)
        val state by viewModel.state.collectAsState()

        OtpVerificationScreen(
            state = state,
            onEvent = viewModel::onEvent,
            onNavigateNext = { isNewUser ->
                if (isNewUser) {
                    navController.navigate(Screen.Registration.route)
                } else {
                    navController.navigate(Screen.CommunitySelection.route)
                }
            }
        )
    }

    composable(Screen.Registration.route) { backStackEntry ->
        val parentEntry = remember(backStackEntry) {
            navController.getBackStackEntry(Screen.Login.route)
        }
        val viewModel: AuthViewModel = hiltViewModel(parentEntry)
        val state by viewModel.state.collectAsState()

        RegistrationScreen(
            state = state,
            onEvent = viewModel::onEvent,
            onNavigateNext = {
                navController.navigate(Screen.CommunitySelection.route)
            }
        )
    }

    composable(Screen.CommunitySelection.route) { backStackEntry ->
        val parentEntry = remember(backStackEntry) {
            navController.getBackStackEntry(Screen.Login.route)
        }
        val viewModel: AuthViewModel = hiltViewModel(parentEntry)
        val state by viewModel.state.collectAsState()

        CommunitySelectionScreen(
            state = state,
            onEvent = viewModel::onEvent,
            onNavigateNext = {
                navController.navigate(Screen.FlatVerification.route)
            }
        )
    }

    composable(Screen.FlatVerification.route) { backStackEntry ->
        val parentEntry = remember(backStackEntry) {
            navController.getBackStackEntry(Screen.Login.route)
        }
        val viewModel: AuthViewModel = hiltViewModel(parentEntry)
        val state by viewModel.state.collectAsState()

        FlatVerificationScreen(
            state = state,
            onEvent = viewModel::onEvent,
            onNavigateNext = { role ->
                onAuthComplete(role)
            }
        )
    }
}
