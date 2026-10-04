package com.sachinkanna.civora.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sachinkanna.civora.ui.launch.CivoraLaunchScreen

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
                    // Route to Onboarding / Auth when implemented
                }
            )
        }
    }
}
