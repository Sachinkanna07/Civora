package com.sachinkanna.civora.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sachinkanna.civora.ui.auth.AuthWelcomeScreen
import com.sachinkanna.civora.ui.auth.LoginScreen
import com.sachinkanna.civora.ui.auth.RegisterScreen
import com.sachinkanna.civora.ui.launch.CivoraLaunchScreen
import com.sachinkanna.civora.ui.onboarding.OnboardingScreen
import com.sachinkanna.civora.viewmodel.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.sachinkanna.civora.ui.dashboard.RoleDashboard
import com.sachinkanna.civora.ui.student.StudentShell
import com.sachinkanna.civora.ui.student.AnnouncementFeed
import com.sachinkanna.civora.ui.student.AnnouncementDetail
import com.sachinkanna.civora.ui.student.EventsFeed
import com.sachinkanna.civora.ui.student.EventDetail
import com.sachinkanna.civora.viewmodel.AnnouncementViewModel
import com.sachinkanna.civora.viewmodel.EventsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CivoraNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Launch.route,
    authViewModel: AuthViewModel = viewModel()
) {
    val authState = authViewModel.state.collectAsState().value
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
                    navController.navigate(Screen.Login.route)
                },
                onCreateAccountClick = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onBackClick = { navController.popBackStack() },
                onCreateAccountClick = {
                    navController.navigate(Screen.Register.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onAuth = { email, password -> authViewModel.login(email, password) }
                , error = authState.error, loading = authState.loading
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onBackClick = { navController.popBackStack() },
                onSignInClick = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onAuth = { name, email, password, role -> authViewModel.register(name, email, password, role) }
                , error = authState.error, loading = authState.loading
            )
        }
        composable(Screen.StudentDashboard.route) { authState.profile?.let { profile -> StudentShell(profile, onLogout = { authViewModel.logout(); navController.navigate(Screen.AuthWelcome.route) { popUpTo(0) { inclusive = true } } }, onAnnouncements = { navController.navigate(Screen.Announcements.route) }, onEvents = { navController.navigate(Screen.Events.route) }) } }
        composable(Screen.Announcements.route) { val vm: AnnouncementViewModel = viewModel(); AnnouncementFeed(vm,{navController.popBackStack()}) { navController.navigate("${Screen.AnnouncementDetail.route}/${it.id}") } }
        composable("${Screen.AnnouncementDetail.route}/{id}") { entry -> val vm: AnnouncementViewModel = viewModel(); val id=entry.arguments?.getString("id").orEmpty(); androidx.compose.runtime.LaunchedEffect(id){vm.loadDetail(id)}; val item = vm.state.collectAsState().value.items.firstOrNull(); item?.let { AnnouncementDetail(it){navController.popBackStack()} } ?: androidx.compose.material3.CircularProgressIndicator() }
        composable(Screen.Events.route) { val vm: EventsViewModel = viewModel(); authState.profile?.let { EventsFeed(vm,it.uid,{navController.popBackStack()}) { navController.navigate("${Screen.EventDetail.route}/${it.id}") } } }
        composable("${Screen.EventDetail.route}/{id}") { entry -> val vm: EventsViewModel = viewModel(); authState.profile?.let { profile -> val id=entry.arguments?.getString("id").orEmpty(); androidx.compose.runtime.LaunchedEffect(id){vm.loadDetail(id,profile.uid)}; val item = vm.state.collectAsState().value.items.firstOrNull(); item?.let { EventDetail(it,vm,profile.uid){navController.popBackStack()} } ?: androidx.compose.material3.CircularProgressIndicator() } }
        composable(Screen.FacultyDashboard.route) { RoleDashboard(com.sachinkanna.civora.data.model.UserRole.FACULTY, authState.profile?.name.orEmpty()) { authViewModel.logout(); navController.navigate(Screen.AuthWelcome.route) { popUpTo(0) } } }
        composable(Screen.AdminDashboard.route) { RoleDashboard(com.sachinkanna.civora.data.model.UserRole.ADMIN, authState.profile?.name.orEmpty()) { authViewModel.logout(); navController.navigate(Screen.AuthWelcome.route) { popUpTo(0) } } }
        composable(Screen.VendorDashboard.route) { RoleDashboard(com.sachinkanna.civora.data.model.UserRole.VENDOR, authState.profile?.name.orEmpty()) { authViewModel.logout(); navController.navigate(Screen.AuthWelcome.route) { popUpTo(0) } } }
        composable(Screen.DriverDashboard.route) { RoleDashboard(com.sachinkanna.civora.data.model.UserRole.DRIVER, authState.profile?.name.orEmpty()) { authViewModel.logout(); navController.navigate(Screen.AuthWelcome.route) { popUpTo(0) } } }
    }
    androidx.compose.runtime.LaunchedEffect(authState.profile?.uid, authState.loading) {
        if (!authState.loading && authState.profile != null && navController.currentDestination?.route !in listOf(Screen.StudentDashboard.route, Screen.FacultyDashboard.route, Screen.AdminDashboard.route, Screen.VendorDashboard.route, Screen.DriverDashboard.route)) {
            val route = when (authState.profile.role) { com.sachinkanna.civora.data.model.UserRole.STUDENT -> Screen.StudentDashboard.route; com.sachinkanna.civora.data.model.UserRole.FACULTY -> Screen.FacultyDashboard.route; com.sachinkanna.civora.data.model.UserRole.ADMIN -> Screen.AdminDashboard.route; com.sachinkanna.civora.data.model.UserRole.VENDOR -> Screen.VendorDashboard.route; com.sachinkanna.civora.data.model.UserRole.DRIVER -> Screen.DriverDashboard.route }
            navController.navigate(route) { popUpTo(0) { inclusive = true } }
        }
    }
}
