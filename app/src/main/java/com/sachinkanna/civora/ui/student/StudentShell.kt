package com.sachinkanna.civora.ui.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sachinkanna.civora.data.model.UserProfile

private enum class StudentTab(val label: String) { HOME("Home"), EXPLORE("Explore"), ACTIVITY("Activity"), COMMUNITY("Community"), PROFILE("Profile") }

@Composable fun StudentShell(profile: UserProfile, onLogout: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(StudentTab.HOME) }
    Scaffold(bottomBar = { NavigationBar { StudentTab.entries.forEach { item -> NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Icon(when(item){StudentTab.HOME->Icons.Default.Home;StudentTab.EXPLORE->Icons.Default.Search;StudentTab.ACTIVITY->Icons.Default.Notifications;StudentTab.COMMUNITY->Icons.Default.Groups;StudentTab.PROFILE->Icons.Default.Person}, item.label) }, label = { Text(item.label) }) } } }) { padding -> Box(Modifier.padding(padding)) { when(tab) { StudentTab.HOME -> Home(profile); StudentTab.EXPLORE -> Explore(); StudentTab.ACTIVITY -> Activity(); StudentTab.COMMUNITY -> Community(); StudentTab.PROFILE -> Profile(profile, onLogout) } } }
}

@Composable private fun Home(profile: UserProfile) { LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) { item { Text("Good morning, ${profile.name.ifBlank { "student" }} 👋", style = MaterialTheme.typography.headlineMedium); Text("What’s happening on campus?", color = MaterialTheme.colorScheme.onSurfaceVariant) }; item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp)) { Text("Civora campus update", style = MaterialTheme.typography.titleLarge); Text("Stay close to the moments, people and places that make college feel like yours.") } } }; item { Text("Quick actions", style = MaterialTheme.typography.titleLarge) }; item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Food", "Bus", "Events").forEach { AssistChip(onClick = {}, label = { Text(it) }) } } }; item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Map", "Hostel", "Report").forEach { AssistChip(onClick = {}, label = { Text(it) }) } } }; item { SectionCard("Upcoming events", "Campus Festival · Tomorrow", "Open Explore to discover what’s next.") }; item { SectionCard("Campus bus", "Route 2 · Next departure in 10 min", "Live tracking will connect here later.") }; item { SectionCard("Recent announcements", "No new notices yet", "Important updates will appear here.") } } }
@Composable private fun Explore() { LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Text("Explore campus", style = MaterialTheme.typography.headlineMedium); OutlinedTextField("", {}, label={Text("Search events, food, clubs…")}, modifier=Modifier.fillMaxWidth()) }; items(listOf("Events" to "Find your next highlight", "Announcements" to "Keep up with campus news", "Food" to "Discover campus bites", "Bus & Map" to "Move around with ease", "Hostel & Reports" to "Get support fast", "Clubs & Community" to "Meet your people")) { (title, desc) -> SectionCard(title, desc, "Coming soon") } } }
@Composable private fun Activity() { EmptyPage("Your activity", "Food orders, event registrations and requests will gather here.") }
@Composable private fun Community() { LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) { item { Text("Find your campus crew", style=MaterialTheme.typography.headlineMedium) }; items(listOf("Clubs", "Department groups", "Campus happenings")) { SectionCard(it, "A welcoming space for campus discovery.", "Foundation ready") } } }
@Composable private fun Profile(profile: UserProfile, onLogout: () -> Unit) { LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) { item { Text("Your profile", style=MaterialTheme.typography.headlineMedium) }; item { Text(profile.name, style=MaterialTheme.typography.titleLarge); Text(profile.email, color=MaterialTheme.colorScheme.onSurfaceVariant); AssistChip(onClick={}, label={Text(profile.role.displayName)}) }; item { Text("Department: ${profile.department.ifBlank { "Not set" }}") }; item { Text("Year: ${profile.year.ifBlank { "Not set" }}") }; item { OutlinedButton(onClick=onLogout, modifier=Modifier.fillMaxWidth()) { Text("Log out") } } } }
@Composable private fun EmptyPage(title:String, body:String) { Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) { Text(title, style=MaterialTheme.typography.headlineMedium); Text(body, color=MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun SectionCard(title:String, subtitle:String, body:String) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement=Arrangement.spacedBy(4.dp)) { Text(title, style=MaterialTheme.typography.titleLarge); Text(subtitle, style=MaterialTheme.typography.titleMedium); Text(body, color=MaterialTheme.colorScheme.onSurfaceVariant) } } }
