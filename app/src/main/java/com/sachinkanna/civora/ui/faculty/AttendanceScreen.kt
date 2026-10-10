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
    var eventId by rememberSaveable {mutableStateOf("")}
    val events=state.values<CampusEvent>(CampusFeed.EVENTS)
    var ticket by rememberSaveable { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }
    if(scanning) TicketScanner(onScanned={ ticket=it; scanning=false },onClose={scanning=false})
    CampusPage("Event attendance", back) {
        item {
            Text(
                "Scan a student ticket, then validate it to record attendance."
            )
            Button(onClick={scanning=true},enabled=!state.busy) { Text("Scan ticket with camera") }
            OutlinedTextField(
                ticket,
                { ticket = it.take(500) },
                label = { Text("Ticket code (fallback)") },
                modifier = Modifier.fillMaxWidth(),
            )
            val parts = parseTicketPayload(ticket)
            Button(
                onClick = {
                    vm.action(
                        "checkIn",
                        mapOf("registrationId" to parts!!.first, "token" to parts.second),
                        "Attendance recorded",
                    )
                },
                enabled = !state.busy && parts != null,
            ) {
                Text("Validate and check in")
            }
        }
        item {SelectField("Event",eventId,events.map {Choice(it.id,it.title,dateTime(it.startTime))},{eventId=it},allowNone=true)}
        feedState(CampusFeed.REGISTRATIONS, state, vm, "No registered students for your events.")
        items(state.values<Ticket>(CampusFeed.REGISTRATIONS).filter {eventId.isBlank()||it.eventId==eventId}, key = { it.id }) { t ->
            CampusCard(t.userName.ifBlank {"Registered student"}, t.eventTitle.ifBlank {events.find {it.id==t.eventId}?.title?:"Campus event"}, if (t.attended) "ATTENDED" else "REGISTERED")
        }
    }
}
