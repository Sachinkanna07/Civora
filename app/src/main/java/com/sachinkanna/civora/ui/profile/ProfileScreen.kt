package com.sachinkanna.civora.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sachinkanna.civora.data.model.*
import com.sachinkanna.civora.data.repository.*
import com.sachinkanna.civora.ui.components.*
import com.sachinkanna.civora.viewmodel.*

@Composable
fun ProfileScreen(
    profile: UserProfile,
    state: WorkspaceState,
    vm: CampusWorkspaceViewModel,
    refresh: () -> Unit,
    logout: () -> Unit,
    open: (String) -> Unit,
) {
    val notificationPermission =
        androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) {}
    var name by rememberSaveable(profile.uid) { mutableStateOf(profile.name) }
    var department by rememberSaveable(profile.uid) { mutableStateOf(profile.department) }
    var year by rememberSaveable(profile.uid) { mutableStateOf(profile.year) }
    var section by rememberSaveable(profile.uid) { mutableStateOf(profile.section) }
    var image by rememberSaveable(profile.uid) { mutableStateOf(profile.profileImage) }
    var notifications by
        rememberSaveable(profile.uid) { mutableStateOf(profile.notificationsEnabled) }
    var routeId by rememberSaveable(profile.uid) { mutableStateOf(profile.routeId) }
    CampusPage("You") {
        if (profile.profileImage.isNotBlank())
            item {
                coil.compose.AsyncImage(profile.profileImage, "Profile photo", Modifier.size(80.dp))
            }
        item { CampusCard(profile.name, profile.email, profile.role.displayName) }
        item {
            Text(
                "Complete your academic details to personalize Today.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            OutlinedTextField(
                name,
                { name = it.take(100) },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                department,
                { department = it.take(80) },
                label = { Text("Department code") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                year,
                { year = it.take(20) },
                label = { Text("Year") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                section,
                { section = it.take(20) },
                label = { Text("Section") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                image,
                { image = it.take(2000) },
                label = { Text("Profile image HTTPS URL (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Text("Your bus route", style = MaterialTheme.typography.titleMedium)
            FlowRow {
                state.values<BusRoute>(CampusFeed.ROUTES).forEach { route ->
                    FilterChip(
                        routeId == route.id,
                        { routeId = route.id },
                        label = { Text(route.name) },
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Push notifications")
                Switch(
                    notifications,
                    {
                        notifications = it
                        if (it && android.os.Build.VERSION.SDK_INT >= 33)
                            notificationPermission.launch(
                                android.Manifest.permission.POST_NOTIFICATIONS
                            )
                    },
                )
            }
        }
        item {
            Button(
                onClick = {
                    vm.saveProfile(
                        mapOf(
                            "name" to name.trim(),
                            "department" to department.trim(),
                            "year" to year.trim(),
                            "section" to section.trim(),
                            "profileImage" to image.trim(),
                            "routeId" to routeId,
                            "notificationsEnabled" to notifications,
                        ),
                        refresh,
                    )
                },
                enabled =
                    !state.busy &&
                        name.trim().length >= 2 &&
                        (image.isBlank() || image.startsWith("https://")),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save profile")
            }
        }
        item {
            TextButton(onClick = { open("Timetable") }) { Text("Weekly timetable") }
            TextButton(onClick = { open("Inbox") }) { Text("Campus inbox") }
            Text("Appearance follows your device theme.")
            OutlinedButton(onClick = logout, modifier = Modifier.fillMaxWidth()) { Text("Log out") }
        }
    }
}
