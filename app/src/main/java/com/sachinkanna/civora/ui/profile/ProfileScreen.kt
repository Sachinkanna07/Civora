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
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    profile: UserProfile,
    state: WorkspaceState,
    vm: CampusWorkspaceViewModel,
    refresh: () -> Unit,
    logout: () -> Unit,
    open: (String) -> Unit,
    back: (() -> Unit)? = null,
) {
    val notificationPermission =
        androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) {}
    var name by rememberSaveable(profile.uid) { mutableStateOf(profile.name) }
    var department by rememberSaveable(profile.uid) { mutableStateOf(profile.department) }
    var year by rememberSaveable(profile.uid) { mutableStateOf(profile.year) }
    var section by rememberSaveable(profile.uid) { mutableStateOf(profile.section) }
    val context=androidx.compose.ui.platform.LocalContext.current
    var stop by rememberSaveable(profile.uid) {mutableStateOf(profile.boardingStop)}
    var theme by rememberSaveable(profile.uid) {mutableStateOf(profile.themePreference)}
    var notifications by
        rememberSaveable(profile.uid) { mutableStateOf(profile.notificationsEnabled) }
    var routeId by rememberSaveable(profile.uid) { mutableStateOf(profile.routeId) }
    var showDeletion by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf(false) }
    var deletionError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    if (showDeletion) AlertDialog(
        onDismissRequest = { if (!deleting) { showDeletion = false; password = "" } },
        title = { Text("Delete your account?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Your profile, tickets, order and meal history, reports and photos will be permanently deleted. Complete or cancel active food orders first. This cannot be undone.")
                OutlinedTextField(password, { password = it }, label = { Text("Confirm password") },
                    singleLine = true, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password), enabled = !deleting)
                deletionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = password.isNotBlank() && !deleting, onClick = {
                deleting = true; deletionError = null
                scope.launch {
                    try { AuthRepository().deleteAccount(password); password = ""; showDeletion = false; logout() }
                    catch (error: Exception) {
                        deletionError = if (error is com.google.firebase.functions.FirebaseFunctionsException) error.message
                            else "Deletion could not be confirmed. Check your password and connection."
                    } finally { deleting = false }
                }
            }) { Text(if (deleting) "Requesting deletion…" else "Delete permanently") }
        },
        dismissButton = { TextButton(enabled = !deleting, onClick = { showDeletion = false; password = "" }) { Text("Keep account") } },
    )
    CampusPage("Profile",back) {
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
            if(profile.role==UserRole.STUDENT) {
            val departments=state.values<Department>(CampusFeed.DEPARTMENTS)
            val selected=departments.find {it.code==department}
            SelectField("Department",department,departments.map {Choice(it.code,it.name)},{department=it;year="";section=""})
            SelectField("Year",year,(selected?.years?:listOf("1","2","3","4")).map {Choice(it,"Year $it")},{year=it})
            SelectField("Section",section,(selected?.sections?:listOf("A","B")).map {Choice(it,"Section $it")},{section=it})
            } else SectionTitle("Staff assignment", "${profile.role.displayName} · ${profile.department.ifBlank {"Department not assigned"}}. Campus operations manages this assignment.")
        }
        item {
            val routes=state.values<BusRoute>(CampusFeed.ROUTES)
            SelectField("Preferred bus route",routeId,routes.map {Choice(it.id,it.name)},{routeId=it;stop=""},allowNone=true)
            routes.find {it.id==routeId}?.let {r->SelectField("Boarding stop",stop,r.stops.map {Choice(it.name,it.name)},{stop=it})}
        }
        item {SelectField("Appearance",theme,listOf(Choice("SYSTEM","Follow device"),Choice("LIGHT","Light"),Choice("DARK","Dark")),{theme=it;com.sachinkanna.civora.ThemePreferences.update(context,it)})}
        item {
            val diagnostics by com.sachinkanna.civora.DiagnosticsPreferences.enabled.collectAsState()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Share crash diagnostics")
                    Text("Optional crash reports help diagnose failures. No analytics or location history is added.", style = MaterialTheme.typography.bodySmall)
                }
                Switch(diagnostics, { com.sachinkanna.civora.DiagnosticsPreferences.update(context,it) })
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
                            "boardingStop" to stop,
                            "themePreference" to theme,
                            "routeId" to routeId,
                            "notificationsEnabled" to notifications,
                        ),
                        refresh,
                    )
                },
                enabled =
                    !state.busy &&
                        name.trim().length >= 2,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save profile")
            }
        }
        item {
            if(profile.role==UserRole.STUDENT) {
            TextButton(onClick = { open("Timetable") }) { Text("Weekly timetable") }
            TextButton(onClick = { open("Inbox") }) { Text("Campus inbox") }
            }

            OutlinedButton(onClick = logout, modifier = Modifier.fillMaxWidth()) { Text("Log out") }
            if(profile.role==UserRole.STUDENT)TextButton(onClick = { deletionError = null; showDeletion = true }) { Text("Delete account", color = MaterialTheme.colorScheme.error) }
            else Text("To delete this staff account, ask campus operations to transfer your responsibilities and remove your account and personal data.",style=MaterialTheme.typography.bodySmall)
        }
    }
}
