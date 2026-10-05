package com.sachinkanna.civora.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sachinkanna.civora.ui.auth.AuthWelcomeScreen
import com.sachinkanna.civora.ui.launch.CivoraLaunchScreen
import com.sachinkanna.civora.ui.onboarding.OnboardingScreen

@Composable
fun CivoraNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Launch.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Launch.route) {
            CivoraLaunchScreen(
                onGetStarted = {
                    navController.navigate(Screen.Onboarding.route)
                }
            )
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
                    // Route to Login in future auth task
                },
                onCreateAccountClick = {
                    // Route to Register in future auth task
                }
            )
        }
    }
}
