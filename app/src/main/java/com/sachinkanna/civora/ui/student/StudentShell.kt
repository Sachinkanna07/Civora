package com.sachinkanna.civora.ui.student

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sachinkanna.civora.data.model.*
import com.sachinkanna.civora.data.repository.*
import com.sachinkanna.civora.ui.activity.ActivityScreen
import com.sachinkanna.civora.ui.announcements.AnnouncementsScreen
import com.sachinkanna.civora.ui.community.ClubsScreen
import com.sachinkanna.civora.ui.components.*
import com.sachinkanna.civora.ui.discover.DiscoverScreen
import com.sachinkanna.civora.ui.events.EventsScreen
import com.sachinkanna.civora.ui.food.FoodScreen
import com.sachinkanna.civora.ui.profile.ProfileScreen
import com.sachinkanna.civora.ui.reports.ReportsScreen
import com.sachinkanna.civora.ui.timetable.TimetableScreen
import com.sachinkanna.civora.ui.today.TodayScreen
import com.sachinkanna.civora.ui.transport.*
import com.sachinkanna.civora.viewmodel.*

private enum class Tab(val label: String) {
    TODAY("Today"),
    DISCOVER("Discover"),
    ACTION("Action"),
    ACTIVITY("Activity"),
    YOU("You"),
}

@Composable
fun StudentShell(
    profile: UserProfile,
    onLogout: () -> Unit,
    onRefreshProfile: () -> Unit = {},
    vm: CampusWorkspaceViewModel = viewModel(),
) {
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    var page by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(profile) { vm.connect(profile) }
    val state by vm.state.collectAsStateWithLifecycle()
    val open: (String) -> Unit = { page = it }
    val back: () -> Unit = { page = null }
    BackHandler(page != null) { back() }
    Scaffold(
        bottomBar = {
            if (page == null)
                NavigationBar {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            tab == t,
                            { tab = t },
                            {
                                Icon(
                                    when (t) {
                                        Tab.TODAY -> Icons.Default.Today
                                        Tab.DISCOVER -> Icons.Default.Explore
                                        Tab.ACTION -> Icons.Default.AddCircle
                                        Tab.ACTIVITY -> Icons.Default.Notifications
                                        Tab.YOU -> Icons.Default.Person
                                    },
                                    t.label,
                                )
                            },
                            label = { Text(t.label) },
                        )
                    }
                }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            ActionMessage(state, vm)
            Box(Modifier.weight(1f)) {
                when {
                    page == "Food" || page?.startsWith("Order:") == true ->
                        FoodScreen(
                            state,
                            vm,
                            back,
                            page?.substringAfter(':')?.takeIf { page?.startsWith("Order:") == true },
                        )
                    page == "Bus" -> TransportScreen(profile, state, vm, back)
                    page == "Timetable" -> TimetableScreen(state, vm, back)
                    page == "Report" || page?.startsWith("Report:") == true ->
                        ReportsScreen(
                            state,
                            vm,
                            back,
                            reportId =
                                page?.substringAfter(':')?.takeIf {
                                    page?.startsWith("Report:") == true
                                },
                        )
                    page == "Events" || page?.startsWith("Event:") == true ->
                        EventsScreen(
                            profile,
                            state,
                            vm,
                            back,
                            page?.substringAfter(':')?.takeIf { page?.startsWith("Event:") == true },
                        )
                    page == "Notices" || page?.startsWith("Notice:") == true ->
                        AnnouncementsScreen(
                            profile,
                            state,
                            vm,
                            back,
                            page?.substringAfter(':')?.takeIf {
                                page?.startsWith("Notice:") == true
                            },
                        )
                    page == "Clubs" -> ClubsScreen(state, vm, back, open)
                    page == "Campus services" -> CampusLocationsScreen(state, vm, back)
                    page == "Inbox" -> ActivityScreen(state, vm, open, true, back)
                    else ->
                        when (tab) {
                            Tab.TODAY -> TodayScreen(profile, state, vm, open)
                            Tab.DISCOVER -> DiscoverScreen(state, vm, open)
                            Tab.ACTIVITY -> ActivityScreen(state, vm, open)
                            Tab.YOU ->
                                ProfileScreen(profile, state, vm, onRefreshProfile, onLogout, open)
                            Tab.ACTION ->
                                CampusPage("What do you need?") {
                                    items(listOf("Food", "Events", "Report", "Bus", "Timetable")) {
                                        action ->
                                        CampusCard(
                                            action,
                                            when (action) {
                                                "Food" -> "Order from a campus canteen"
                                                "Events" -> "Register or show your QR ticket"
                                                "Report" -> "Tell campus operations about an issue"
                                                "Bus" -> "Check routes and current trips"
                                                else -> "Check your next class"
                                            },
                                        ) {
                                            open(action)
                                        }
                                    }
                                }
                        }
                }
            }
        }
    }
}
