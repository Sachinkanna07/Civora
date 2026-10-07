package com.sachinkanna.civora.ui.admin

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
fun InvitationsScreen(state: WorkspaceState, vm: CampusWorkspaceViewModel, back: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var department by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf(UserRole.FACULTY) }
    CampusPage("Invite campus staff", back) {
        item {
            Text(
                "Privileged accounts are provisioned by the backend. Deliver the returned account setup link to the invitee through your approved campus channel."
            )
            OutlinedTextField(
                name,
                { name = it.take(100) },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                email,
                { email = it.take(254) },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                department,
                { department = it.take(80) },
                label = { Text("Department") },
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow {
                listOf(UserRole.FACULTY, UserRole.VENDOR, UserRole.DRIVER).forEach { r ->
                    FilterChip(role == r, { role = r }, label = { Text(r.displayName) })
                }
            }
            Button(
                onClick = {
                    vm.action(
                        "invitePrivilegedUser",
                        mapOf(
                            "name" to name,
                            "email" to email,
                            "role" to role.roleKey,
                            "department" to department,
                        ),
                        "Account provisioned",
                    )
                },
                enabled =
                    !state.busy &&
                        name.isNotBlank() &&
                        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                        (role != UserRole.FACULTY || department.isNotBlank()),
            ) {
                Text("Create invitation")
            }
        }
        state.invitationLink?.let { link ->
            item {
                androidx.compose.foundation.text.selection.SelectionContainer { Text(link) }
                Text(
                    "Setup links grant account access. Share only with the intended recipient.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
