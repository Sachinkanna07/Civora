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
import com.sachinkanna.civora.ui.mess.MessScreen
import com.sachinkanna.civora.ui.reports.ReportsScreen
import com.sachinkanna.civora.ui.reports.MaintenanceScreen
import androidx.compose.ui.platform.LocalContext
import com.sachinkanna.civora.ui.transport.TransportScreen
import com.sachinkanna.civora.viewmodel.*

@Composable
fun RoleDashboard(
    profile: UserProfile,
    onLogout: () -> Unit,
    onRefreshProfile: () -> Unit = {},
    vm: CampusWorkspaceViewModel = viewModel(factory = CampusWorkspaceViewModel.factory(LocalContext.current)),
) {
    LaunchedEffect(profile) { vm.connect(profile); if(profile.role==UserRole.ADMIN)vm.action("getOperationsSummary",emptyMap(),"Operations summary refreshed") }
    val state by vm.state.collectAsStateWithLifecycle()
    var metrics by remember {mutableStateOf<Map<String,Any?>>(emptyMap())}
    LaunchedEffect(state.lastResult) {if(state.lastAction=="getOperationsSummary")metrics=state.lastResult}
    var page by rememberSaveable { mutableStateOf<String?>(null) }
    val logout: () -> Unit = { vm.disconnect(); onLogout() }
    val back: () -> Unit = { page = null }
    BackHandler(page != null) { back() }
    CompositionLocalProvider(LocalAccountAction provides if(page == "Settings") null else ({page = "Settings"})) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            when {
                page == "Settings" -> com.sachinkanna.civora.ui.profile.ProfileScreen(profile,state,vm,onRefreshProfile,logout,{page=it},back)
                profile.role == UserRole.MESS -> MessScreen(profile,state,vm,logout)
                profile.role == UserRole.MAINTENANCE -> MaintenanceScreen(profile,state,vm,logout)
                profile.role == UserRole.VENDOR -> FoodScreen(state, vm, logout, vendor = true)
                profile.role == UserRole.DRIVER -> DriverScreen(state, vm, logout)
                page == "Mess setup" -> MessSetupScreen(state,vm,back)
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
                                if(metrics.isNotEmpty())CampusCard("Campus operations","${metrics["openReports"]} open reports · ${metrics["reviewRequired"]} awaiting review\n${metrics["activeTrips"]} active trips · ${metrics["openOrders"]} kitchen orders\n${metrics["urgentReports"]} active safety reports · ${metrics["meals24h"]} meals in the past 24 hours")
                                else SectionTitle("Campus operations","Refresh to load server counts.")
                                TextButton(onClick={vm.action("getOperationsSummary",emptyMap(),"Operations summary refreshed")},enabled=!state.busy) {Text("Refresh operations summary")}
                            }
                            feedState(CampusFeed.REPORTS, state, vm, "No pending campus reports.")
                            items(listOf("Reports", "Invite", "Campus data", "Transport", "Mess setup")) { action
                                ->
                                CampusCard(
                                    action,
                                    when (action) {
                                        "Reports" -> "Assign issues and record resolutions"
                                        "Invite" -> "Provision campus staff and operators"
                                        "Mess setup" -> "Manage halls, meal windows and entitlements"
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
                        item { TextButton(onClick = logout) { Text("Log out") } }
                    }
            }
        }
        Box(Modifier.align(androidx.compose.ui.Alignment.BottomCenter)) {ActionMessage(state,vm)}
    }
    }
}
