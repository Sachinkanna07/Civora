package com.sachinkanna.civora.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sachinkanna.civora.data.model.*
import com.sachinkanna.civora.data.repository.*
import com.sachinkanna.civora.ui.admin.*
import com.sachinkanna.civora.ui.components.*
import com.sachinkanna.civora.ui.driver.DriverScreen
import com.sachinkanna.civora.ui.faculty.*
import com.sachinkanna.civora.ui.food.FoodScreen
import com.sachinkanna.civora.ui.reports.ReportsScreen
import com.sachinkanna.civora.ui.transport.TransportScreen
import com.sachinkanna.civora.viewmodel.*

@Composable
fun RoleDashboard(
    profile: UserProfile,
    onLogout: () -> Unit,
    vm: CampusWorkspaceViewModel = viewModel(),
) {
    LaunchedEffect(profile) { vm.connect(profile) }
    val state by vm.state.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf<String?>(null) }
    val back: () -> Unit = { page = null }
    BackHandler(page != null) { back() }
    Column(Modifier.fillMaxSize()) {
        ActionMessage(state, vm)
        Box(Modifier.weight(1f)) {
            when {
                profile.role == UserRole.VENDOR -> FoodScreen(state, vm, onLogout, vendor = true)
                profile.role == UserRole.DRIVER -> DriverScreen(state, vm, onLogout)
                page == "Reports" -> ReportsScreen(state, vm, back, admin = true)
                page == "Publish" -> ContentEditor(profile, state, vm, back)
                page == "Attendance" -> AttendanceScreen(state, vm, back)
                page == "Invite" -> InvitationsScreen(state, vm, back)
                page == "Campus data" -> CampusDataScreen(state, vm, back)
                page == "Transport" -> TransportScreen(profile, state, vm, back)
                else ->
                    CampusPage("${profile.role.displayName} workspace") {
                        item {
                            Text(
                                "Welcome, ${profile.name}",
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                        if (profile.role == UserRole.FACULTY)
                            item {
                                CampusCard(
                                    profile.department.ifBlank { "Department not assigned" },
                                    "${state.values<CampusEvent>(CampusFeed.EVENTS).count {it.createdBy==profile.uid}} upcoming events you manage | ${state.values<Announcement>(CampusFeed.ANNOUNCEMENTS).size} relevant campus notices",
                                )
                            }
                        if (profile.role == UserRole.ADMIN) {
                            item {
                                CampusCard(
                                    "Campus operations",
                                    "${state.values<CampusReport>(CampusFeed.REPORTS).count {it.status!=ReportStatus.RESOLVED&&it.status!=ReportStatus.REJECTED}} open reports | ${state.values<ActiveTrip>(CampusFeed.TRIPS).size} active trips\n${state.values<Canteen>(CampusFeed.CANTEENS).size} canteens | ${state.values<CampusReport>(CampusFeed.REPORTS).count {it.category==ReportCategory.SAFETY&&it.status!=ReportStatus.RESOLVED&&it.status!=ReportStatus.REJECTED}} active safety incidents",
                                )
                            }
                            feedState(CampusFeed.REPORTS, state, vm, "No pending campus reports.")
                            items(listOf("Reports", "Invite", "Campus data", "Transport")) { action
                                ->
                                CampusCard(
                                    action,
                                    when (action) {
                                        "Reports" -> "Assign issues and record resolutions"
                                        "Invite" -> "Provision faculty, vendor and driver accounts"
                                        "Campus data" ->
                                            "Manage schedules, venues, menus and bus assignments"
                                        else -> "Review routes and active trips"
                                    },
                                ) {
                                    page = action
                                }
                            }
                        }
                        item {
                            CampusCard("Publish", "Create or edit announcements and events") {
                                page = "Publish"
                            }
                        }
                        item {
                            CampusCard(
                                "Attendance",
                                "Review registrations and validate event tickets",
                            ) {
                                page = "Attendance"
                            }
                        }
                        feedState(CampusFeed.EVENTS, state, vm, "No upcoming campus events.")
                        item { TextButton(onClick = onLogout) { Text("Log out") } }
                    }
            }
        }
    }
}
