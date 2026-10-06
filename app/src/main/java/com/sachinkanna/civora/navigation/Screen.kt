package com.sachinkanna.civora.navigation

/**
 * Route definitions for Civora navigation.
 */
sealed class Screen(val route: String) {
    data object Launch : Screen("launch")
    data object Onboarding : Screen("onboarding")
    data object AuthWelcome : Screen("auth_welcome")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object RoleSelection : Screen("role_selection")
    data object StudentDashboard : Screen("student_dashboard")
    data object FacultyDashboard : Screen("faculty_dashboard")
    data object AdminDashboard : Screen("admin_dashboard")
    data object VendorDashboard : Screen("vendor_dashboard")
    data object DriverDashboard : Screen("driver_dashboard")
    data object Announcements : Screen("announcements")
    data object AnnouncementDetail : Screen("announcement_detail")
    data object Events : Screen("events")
    data object EventDetail : Screen("event_detail")
}
