package com.sachinkanna.civora.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.sachinkanna.civora.data.model.UserRole
import com.sachinkanna.civora.ui.auth.*
import com.sachinkanna.civora.ui.dashboard.RoleDashboard
import com.sachinkanna.civora.ui.launch.CivoraLaunchScreen
import com.sachinkanna.civora.ui.onboarding.OnboardingScreen
import com.sachinkanna.civora.ui.student.StudentShell
import com.sachinkanna.civora.viewmodel.AuthViewModel

@Composable
fun CivoraNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Launch.route,
    authViewModel: AuthViewModel = viewModel(),
) {
    val state by authViewModel.state.collectAsState()
    val logout: () -> Unit = {
        authViewModel.logout()
        navController.navigate(Screen.AuthWelcome.route) { popUpTo(0) { inclusive = true } }
    }
    NavHost(navController, startDestination, modifier) {
        composable(Screen.Launch.route) {
            CivoraLaunchScreen(onGetStarted = { navController.navigate(Screen.Onboarding.route) })
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinishOnboarding = {
                    navController.navigate(Screen.AuthWelcome.route) {
                        popUpTo(Screen.Launch.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.AuthWelcome.route) {
            AuthWelcomeScreen(
                onSignInClick = {
                    authViewModel.clearError()
                    navController.navigate(Screen.Login.route)
                },
                onCreateAccountClick = {
                    authViewModel.clearError()
                    navController.navigate(Screen.Register.route)
                },
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onBackClick = { navController.popBackStack() },
                onCreateAccountClick = {
                    authViewModel.clearError()
                    navController.navigate(Screen.Register.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onAuth = { email, password -> authViewModel.login(email, password) },
                error = state.error,
                loading = state.loading,
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onBackClick = { navController.popBackStack() },
                onSignInClick = {
                    authViewModel.clearError()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onAuth = { name, email, password -> authViewModel.register(name, email, password) },
                error = state.error,
                loading = state.loading,
            )
        }
        composable("workspace") {
            state.profile?.let { profile ->
                if (profile.role == UserRole.STUDENT)
                    StudentShell(
                        profile,
                        logout,
                        onRefreshProfile = { authViewModel.refreshProfile() },
                    )
                else RoleDashboard(profile, logout)
            }
        }
    }
    LaunchedEffect(state.profile?.uid, state.loading) {
        if (
            !state.loading &&
                state.profile != null &&
                navController.currentDestination?.route != "workspace"
        )
            navController.navigate("workspace") { popUpTo(0) { inclusive = true } }
    }
}
