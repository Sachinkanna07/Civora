package com.sachinkanna.civora.ui.onboarding

import androidx.compose.ui.graphics.Color
import com.sachinkanna.civora.ui.theme.Amber500
import com.sachinkanna.civora.ui.theme.Cyan400
import com.sachinkanna.civora.ui.theme.Emerald500
import com.sachinkanna.civora.ui.theme.Rose500
import com.sachinkanna.civora.ui.theme.Violet400

/**
 * Model representing a single page in the onboarding flow.
 */
data class OnboardingPageData(
    val id: Int,
    val badge: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val highlights: List<String>,
    val accentColor: Color
)

object OnboardingPages {
    val list = listOf(
        OnboardingPageData(
            id = 1,
            badge = "WELCOME TO CIVORA",
            title = "Welcome to Civora",
            subtitle = "Everything on campus. One app.",
            description = "Experience a unified digital campus. Civora connects students, faculty, admin, vendors, and drivers into a seamless, modern super-app.",
            highlights = listOf(
                "Unified Digital Campus Infrastructure",
                "Tailored Roles for Every Campus User",
                "Fast, Modern & Secure Operations"
            ),
            accentColor = Cyan400
        ),
        OnboardingPageData(
            id = 2,
            badge = "REAL-TIME ALERTS",
            title = "Stay Updated",
            subtitle = "Never miss what matters.",
            description = "Get instant official department announcements, upcoming campus event schedules, class changes, and urgent broadcast notifications directly.",
            highlights = listOf(
                "Instant Official Department Alerts",
                "Interactive Campus Event Calendar",
                "Urgent Safety & Emergency Broadcasts"
            ),
            accentColor = Violet400
        ),
        OnboardingPageData(
            id = 3,
            badge = "SMART UTILITIES",
            title = "Campus Services",
            subtitle = "Effortless daily tasks.",
            description = "Order meals from campus canteens without waiting in line, submit hostel maintenance requests, and lodge grievances with direct resolution tracking.",
            highlights = listOf(
                "Smart Canteen Food Pre-Ordering",
                "Hostel Facilities & Maintenance Requests",
                "Transparent Campus Issue Reporting"
            ),
            accentColor = Emerald500
        ),
        OnboardingPageData(
            id = 4,
            badge = "LIVE STATUS",
            title = "Track in Real Time",
            subtitle = "Campus mobility & updates.",
            description = "Track live campus bus locations on map routes, monitor food order preparation in real time, and follow request resolution status.",
            highlights = listOf(
                "Live Campus Bus GPS Tracking",
                "Real-time Food Order Status",
                "Live Request Progress Timelines"
            ),
            accentColor = Amber500
        ),
        OnboardingPageData(
            id = 5,
            badge = "CAMPUS ECOSYSTEM",
            title = "One Campus Community",
            subtitle = "Built for everyone on campus.",
            description = "Civora connects Students, Faculty, Admins, Canteen Vendors, and Bus Drivers to streamline communication and daily workflow.",
            highlights = listOf(
                "Role-Specific Dashboards & Tools",
                "Cross-Departmental Collaboration",
                "Portfolio-Grade Reliability & Speed"
            ),
            accentColor = Rose500
        )
    )
}
