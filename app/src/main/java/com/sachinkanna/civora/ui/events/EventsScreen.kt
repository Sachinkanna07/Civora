package com.sachinkanna.civora.ui.events

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.sachinkanna.civora.data.model.*
import com.sachinkanna.civora.data.repository.*
import com.sachinkanna.civora.ui.components.*
import com.sachinkanna.civora.viewmodel.*

@Composable
fun EventsScreen(
    profile: UserProfile,
    state: WorkspaceState,
    vm: CampusWorkspaceViewModel,
    back: () -> Unit,
    eventId: String? = null,
) {
    var selected by rememberSaveable(eventId) { mutableStateOf(eventId) }
    var myEvents by rememberSaveable { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<CampusEvent?>(null) }
    var error by remember { mutableStateOf(false) }
    var detailLoading by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(selected, retry) {
        detail = null
        error = false
        if (selected != null) {
            detailLoading = true
            try {
                detail = EventsRepository().get(selected!!)
            } catch (e: Exception) {
                error = true
            } finally {
                detailLoading = false
            }
        }
    }
    val events = state.values<CampusEvent>(CampusFeed.EVENTS)
    val tickets = state.values<Ticket>(CampusFeed.REGISTRATIONS)
    val event = events.find { it.id == selected } ?: detail
    val ticket = tickets.find { it.eventId == selected }
    CampusPage(event?.title ?: "Events", { if (selected != null) selected = null else back() }) {
        if (selected != null) {
            if (detailLoading && event == null) item { CircularProgressIndicator() }
            if (error && event == null)
                item {
                    Text("Could not load this event.")
                    TextButton(onClick = { retry++ }) { Text("Retry") }
                }
            event?.let { e ->
                item {
                    if (e.imageUrl.isNotBlank())
                        coil.compose.AsyncImage(
                            e.imageUrl,
                            "Event image",
                            Modifier.fillMaxWidth().height(200.dp),
                        )
                    Text(e.category, color = MaterialTheme.colorScheme.primary)
                    Text(dateTime(e.startTime))
                    Text("${e.venue} | ${e.organizer}")
                    Text(e.description)
                    Text(
                        if (e.capacity == 0) "Open capacity"
                        else
                            "${(e.capacity-e.registeredCount).coerceAtLeast(0)} seats available / ${e.capacity}"
                    )
                }
                if (profile.role == UserRole.STUDENT)
                    item {
                        Button(
                            onClick = {
                                vm.action(
                                    "eventRegistration",
                                    mapOf("eventId" to e.id, "cancel" to (ticket != null)),
                                    if (ticket == null) "Registered. Your ticket will appear below."
                                    else "Registration cancelled",
                                )
                            },
                            enabled =
                                !state.busy &&
                                    (ticket != null ||
                                        e.registrationEnabled &&
                                            e.startTime > System.currentTimeMillis()) &&
                                    ticket?.attended != true,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (ticket == null) "Register"
                                else if (ticket.attended) "Attendance recorded"
                                else "Cancel registration"
                            )
                        }
                    }
                ticket?.let { t ->
                    item {
                        Text("Your event ticket", style = MaterialTheme.typography.titleMedium)
                        TicketQr(t)
                        Text(
                            "Show this ticket to the organizer. Your token is verified by the server.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        } else {
            item {
                OutlinedTextField(
                    search,
                    { search = it },
                    label = { Text("Search events") },
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow {
                    FilterChip(!myEvents, { myEvents = false }, label = { Text("Discover") })
                    FilterChip(myEvents, { myEvents = true }, label = { Text("My Events") })
                }
                FlowRow {
                    FilterChip(category == null, { category = null }, label = { Text("All") })
                    events
                        .map { it.category }
                        .distinct()
                        .forEach { c ->
                            FilterChip(category == c, { category = c }, label = { Text(c) })
                        }
                }
            }
            feedState(CampusFeed.EVENTS, state, vm, "No upcoming events.")
            val filtered = events.filter {
                (!myEvents || tickets.any { t -> t.eventId == it.id }) &&
                    (category == null || it.category == category) &&
                    "${it.title} ${it.category}".contains(search, true)
            }
            items(filtered, key = { it.id }) { e ->
                CampusCard(
                    e.title,
                    "${dateTime(e.startTime)} | ${e.venue}",
                    if (tickets.any { it.eventId == e.id }) "REGISTERED" else e.category,
                ) {
                    selected = e.id
                }
            }
            if (myEvents)
                items(
                    tickets.filter { t -> events.none { it.id == t.eventId } },
                    key = { "ticket${it.id}" },
                ) { t ->
                    CampusCard(
                        "Registered event",
                        "View event and ticket",
                        if (t.attended) "ATTENDED" else "REGISTERED",
                    ) {
                        selected = t.eventId
                    }
                }
            if (filtered.isEmpty()) item { Text("No events match your filters.") }
        }
    }
}

@Composable
private fun TicketQr(ticket: Ticket) {
    val bitmap =
        remember(ticket.token) {
            val matrix =
                com.google.zxing
                    .MultiFormatWriter()
                    .encode(
                        "civora:${ticket.id}:${ticket.token}",
                        com.google.zxing.BarcodeFormat.QR_CODE,
                        512,
                        512,
                    )
            android.graphics.Bitmap.createBitmap(512, 512, android.graphics.Bitmap.Config.ARGB_8888)
                .apply {
                    for (x in 0 until 512) for (y in 0 until 512) setPixel(
                        x,
                        y,
                        if (matrix[x, y]) android.graphics.Color.BLACK
                        else android.graphics.Color.WHITE,
                    )
                }
        }
    Image(bitmap.asImageBitmap(), "Event check-in QR ticket", Modifier.size(256.dp))
    Text(
        if (ticket.attended) "Checked in" else "Valid registration",
        color = MaterialTheme.colorScheme.primary,
    )
}
