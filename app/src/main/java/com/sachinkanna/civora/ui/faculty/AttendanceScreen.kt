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
fun AttendanceScreen(state: WorkspaceState, vm: CampusWorkspaceViewModel, back: () -> Unit) {
    var ticket by rememberSaveable { mutableStateOf("") }
    CampusPage("Event attendance", back) {
        item {
            Text(
                "Scan the student's ticket with your camera scanner and paste the ticket code here to record attendance."
            )
            OutlinedTextField(
                ticket,
                { ticket = it.take(500) },
                label = { Text("civora:registrationId:token") },
                modifier = Modifier.fillMaxWidth(),
            )
            val parts = ticket.split(':')
            Button(
                onClick = {
                    vm.action(
                        "checkIn",
                        mapOf("registrationId" to parts[1], "token" to parts[2]),
                        "Attendance recorded",
                    )
                },
                enabled = !state.busy && parts.size == 3 && parts[0] == "civora",
            ) {
                Text("Validate and check in")
            }
        }
        feedState(CampusFeed.REGISTRATIONS, state, vm, "No registered students for your events.")
        items(state.values<Ticket>(CampusFeed.REGISTRATIONS), key = { it.id }) { t ->
            CampusCard(t.id, "Event ${t.eventId}", if (t.attended) "ATTENDED" else "REGISTERED")
        }
    }
}
