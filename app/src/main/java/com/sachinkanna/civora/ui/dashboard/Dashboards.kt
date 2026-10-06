package com.sachinkanna.civora.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sachinkanna.civora.data.model.UserRole

@Composable fun RoleDashboard(role: UserRole, name: String, onLogout: () -> Unit) {
    if (role == UserRole.STUDENT) StudentDashboard(name, onLogout) else TemporaryDashboard(role, name, onLogout)
}
@Composable private fun StudentDashboard(name: String, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Hey, ${name.ifBlank { "student" }} 👋", style = MaterialTheme.typography.headlineMedium); TextButton(onClick = onLogout) { Text("Log out") } }
        Text("Your campus, alive.", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp)) { Text("What’s happening on campus?", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(8.dp)); Text("Stay in the loop with announcements, events and your campus crew.") } }
        Text("Quick actions", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("🍔 Food", "🚌 Bus", "🎉 Events").forEach { AssistChip(onClick = {}, label = { Text(it) }) } }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("🗺 Map", "🏠 Hostel", "🚨 Report").forEach { AssistChip(onClick = {}, label = { Text(it) }) } }
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp)) { Text("Coming up next", style = MaterialTheme.typography.titleMedium); Text("Campus festival · Tomorrow"); Text("Next bus · 10 min") } }
    }
}
@Composable private fun TemporaryDashboard(role: UserRole, name: String, onLogout: () -> Unit) { Column(Modifier.fillMaxSize().padding(24.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${role.displayName} dashboard", style = MaterialTheme.typography.headlineMedium); TextButton(onClick = onLogout) { Text("Log out") } }; Spacer(Modifier.height(20.dp)); Text("Welcome, ${name.ifBlank { role.displayName }}."); Spacer(Modifier.height(12.dp)); Text("Your Civora workspace is being prepared.") } }
