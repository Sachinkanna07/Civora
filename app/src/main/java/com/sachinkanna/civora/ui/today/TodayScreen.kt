package com.sachinkanna.civora.ui.today

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.sachinkanna.civora.data.model.*
import com.sachinkanna.civora.data.repository.*
import com.sachinkanna.civora.ui.components.*
import com.sachinkanna.civora.viewmodel.*
import java.time.LocalDateTime
import kotlinx.coroutines.delay

@Composable
fun TodayScreen(
    profile: UserProfile,
    state: WorkspaceState,
    vm: CampusWorkspaceViewModel,
    open: (String) -> Unit,
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = LocalDateTime.now()
        }
    }
    val next = nextClass(state.values<TimetableEntry>(CampusFeed.TIMETABLE), now)
    val notice =
        state
            .values<Announcement>(CampusFeed.ANNOUNCEMENTS)
            .filter {
                (it.expiresAt == null || it.expiresAt > System.currentTimeMillis()) &&
                    (it.department.isBlank() || it.department == profile.department)
            }
            .sortedWith(
                compareByDescending<Announcement> { it.priority in listOf("urgent", "high") }
                    .thenByDescending { it.createdAt }
            )
            .firstOrNull()
    val tickets = state.values<Ticket>(CampusFeed.REGISTRATIONS).map { it.eventId }.toSet()
    val event =
        state
            .values<CampusEvent>(CampusFeed.EVENTS)
            .filter { it.id in tickets && it.startTime >= System.currentTimeMillis() }
            .minByOrNull { it.startTime }
    val order = state.values<FoodOrder>(CampusFeed.ORDERS).firstOrNull { it.status.active }
    val route = state.values<BusRoute>(CampusFeed.ROUTES).find { it.id == profile.routeId }
    val trip = state.values<ActiveTrip>(CampusFeed.TRIPS).find { it.routeId == profile.routeId }
    CampusPage("Today") {
        item {
            Text(
                "${now.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}, ${now.dayOfMonth} ${now.month.name.lowercase().replaceFirstChar { it.uppercase() }}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Hello, ${profile.name.substringBefore(' ')}",
                style = MaterialTheme.typography.titleLarge,
            )
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Food", "Bus", "Events", "Timetable", "Report").forEach { label ->
                    AssistChip(onClick = { open(label) }, label = { Text(label) })
                }
            }
        }
        feedState(
            CampusFeed.TIMETABLE,
            state,
            vm,
            "Your schedule will appear when your department, year and section are assigned.",
        )
        item {
            CampusCard(
                next?.entry?.courseName ?: "No upcoming classes",
                next?.let {
                    "${it.date.dayOfWeek} | ${clockTime(it.entry.startMinute)}-${clockTime(it.entry.endMinute)}\n${it.entry.room} | ${it.entry.faculty}"
                } ?: "Enjoy your free time. View your weekly timetable.",
                "NEXT CLASS",
            ) {
                open("Timetable")
            }
        }
        feedState(CampusFeed.TRIPS, state, vm, "No active campus trips.")
        item {
            CampusCard(
                route?.name ?: "Choose your bus route",
                trip?.location?.let {
                    "${route?.stops?.firstOrNull()?.let { stop -> etaMinutes(it,stop,System.currentTimeMillis()) }?.let { eta -> "Approx. $eta min to first stop" } ?: if(isLocationFresh(it,System.currentTimeMillis())) "Location available" else "Location is stale"} | Updated ${dateTime(it.updatedAt)}"
                } ?: "Live location is unavailable. Check routes and stops.",
                "TRANSPORT",
            ) {
                open("Bus")
            }
        }
        feedState(CampusFeed.ANNOUNCEMENTS, state, vm, "No active campus notices.")
        notice?.let {
            item {
                CampusCard(it.title, it.body.take(160), it.priority.uppercase()) { open("Notices") }
            }
        }
        feedState(
            CampusFeed.REGISTRATIONS,
            state,
            vm,
            "Find an event in Discover and register to see it here.",
        )
        event?.let {
            item {
                CampusCard(it.title, "${dateTime(it.startTime)} | ${it.venue}", "YOUR NEXT EVENT") {
                    open("Events")
                }
            }
        }
        feedState(CampusFeed.ORDERS, state, vm, "No food orders yet.")
        order?.let {
            item {
                CampusCard(
                    "Order ${it.token}",
                    "${money(it.totalPaise)} | Pay at counter",
                    it.status.name,
                ) {
                    open("Food")
                }
            }
        }
        item { TextButton(onClick = { open("Inbox") }) { Text("Open campus inbox") } }
    }
}
