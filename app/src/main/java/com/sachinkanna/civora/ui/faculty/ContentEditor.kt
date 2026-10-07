package com.sachinkanna.civora.ui.faculty

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.sachinkanna.civora.data.model.*
import com.sachinkanna.civora.data.repository.*
import com.sachinkanna.civora.ui.components.*
import com.sachinkanna.civora.viewmodel.*

@Composable
fun ContentEditor(
    profile: UserProfile,
    state: WorkspaceState,
    vm: CampusWorkspaceViewModel,
    back: () -> Unit,
) {
    var event by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("General") }
    var department by rememberSaveable { mutableStateOf(profile.department) }
    var venue by rememberSaveable { mutableStateOf("") }
    var capacity by rememberSaveable { mutableStateOf("0") }
    var start by rememberSaveable { mutableStateOf("") }
    var end by rememberSaveable { mutableStateOf("") }
    var expiry by rememberSaveable { mutableStateOf("") }
    var image by rememberSaveable { mutableStateOf("") }
    var clubId by rememberSaveable { mutableStateOf("") }
    var editingId by rememberSaveable { mutableStateOf("") }
    val startMillis = parseDate(start)
    val endMillis = parseDate(end)
    CampusPage("Publish campus update", back) {
        item {
            FlowRow {
                FilterChip(
                    !event,
                    {
                        event = false
                        category = "General"
                        editingId = ""
                    },
                    label = { Text("Announcement") },
                )
                FilterChip(
                    event,
                    {
                        event = true
                        category = "Workshop"
                        editingId = ""
                    },
                    label = { Text("Event") },
                )
            }
        }
        item {
            OutlinedTextField(
                title,
                { title = it.take(120) },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                body,
                { body = it.take(4000) },
                label = { Text("Details") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
        }
        item {
            if (event)
                OutlinedTextField(
                    category,
                    { category = it.take(30) },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth(),
                )
            else
                FlowRow {
                    listOf("General", "Academic", "Department")
                        .plus(if (profile.role == UserRole.ADMIN) listOf("Urgent") else emptyList())
                        .forEach { c ->
                            FilterChip(category == c, { category = c }, label = { Text(c) })
                        }
                }
        }
        if (profile.role == UserRole.ADMIN)
            item {
                OutlinedTextField(
                    department,
                    { department = it.take(80) },
                    label = { Text("Department (blank = campus-wide)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        if (event) {
            item {
                OutlinedTextField(
                    venue,
                    { venue = it.take(200) },
                    label = { Text("Venue") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    capacity,
                    { capacity = it },
                    label = { Text("Capacity (0 = unlimited)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    start,
                    { start = it },
                    label = { Text("Starts: yyyy-MM-dd HH:mm") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    end,
                    { end = it },
                    label = { Text("Ends: yyyy-MM-dd HH:mm") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Times use the device timezone.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    image,
                    { image = it.take(2000) },
                    label = { Text("Image HTTPS URL (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    clubId,
                    { clubId = it.take(128) },
                    label = { Text("Club ID (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else
            item {
                OutlinedTextField(
                    expiry,
                    { expiry = it },
                    label = { Text("Expires: yyyy-MM-dd HH:mm (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        item {
            Button(
                onClick = {
                    vm.action(
                        "publishCampusContent",
                        mapOf(
                            "kind" to if (event) "event" else "announcement",
                            "id" to editingId.takeIf { it.isNotBlank() },
                            "title" to title,
                            "body" to body,
                            "category" to category,
                            "priority" to if (category == "Urgent") "urgent" else "normal",
                            "department" to department,
                            "venue" to venue,
                            "capacity" to capacity.toIntOrNull(),
                            "startTime" to startMillis,
                            "endTime" to endMillis,
                            "expiresAt" to parseDate(expiry),
                            "imageUrl" to image,
                            "clubId" to clubId,
                        ),
                        "Campus update published",
                    )
                },
                enabled =
                    !state.busy &&
                        title.isNotBlank() &&
                        body.isNotBlank() &&
                        (!event ||
                            venue.isNotBlank() &&
                                capacity.toIntOrNull() != null &&
                                startMillis != null &&
                                endMillis != null) &&
                        (expiry.isBlank() || parseDate(expiry) != null) &&
                        (image.isBlank() || image.startsWith("https://")),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (editingId.isBlank()) "Publish" else "Save changes")
            }
        }
        item { Text("Manage your published updates", style = MaterialTheme.typography.titleLarge) }
        if (event)
            items(
                state.values<CampusEvent>(CampusFeed.EVENTS).filter {
                    profile.role == UserRole.ADMIN || it.createdBy == profile.uid
                },
                key = { it.id },
            ) { e ->
                CampusCard(e.title, dateTime(e.startTime), "EDIT") {
                    editingId = e.id
                    title = e.title
                    body = e.description
                    category = e.category
                    department = e.department
                    venue = e.venue
                    capacity = e.capacity.toString()
                    start = editDate(e.startTime)
                    end = editDate(e.endTime)
                    image = e.imageUrl
                    clubId = e.clubId
                }
            }
        else
            items(
                state.values<Announcement>(CampusFeed.ANNOUNCEMENTS).filter {
                    profile.role == UserRole.ADMIN || it.authorId == profile.uid
                },
                key = { it.id },
            ) { n ->
                CampusCard(n.title, n.category, "EDIT") {
                    editingId = n.id
                    title = n.title
                    body = n.body
                    category = n.category
                    department = n.department
                    expiry = n.expiresAt?.let(::editDate).orEmpty()
                }
            }
    }
}

fun parseDate(text: String): Long? =
    try {
        java.time.LocalDateTime.parse(
                text,
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            )
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    } catch (e: Exception) {
        null
    }

fun editDate(time: Long): String =
    java.time.Instant.ofEpochMilli(time)
        .atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
