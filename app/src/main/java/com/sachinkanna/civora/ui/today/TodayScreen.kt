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
    var now by remember { mutableStateOf(com.sachinkanna.civora.data.model.CampusPolicy.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = com.sachinkanna.civora.data.model.CampusPolicy.now()
        }
    }
    val timetable=state.values<TimetableEntry>(CampusFeed.TIMETABLE)
    val overrides=state.values<AcademicOverride>(CampusFeed.ACADEMIC_OVERRIDES)
    val holidays=holidayDates(state.values<CalendarDay>(CampusFeed.CALENDAR),profile.department)
    val todayClasses=resolvedClassesForDate(timetable,overrides,now.toLocalDate())
    val current=if(now.toLocalDate() in holidays)null else currentClass(todayClasses,now)
    val next = nextAcademicClass(timetable,overrides,now,holidays)
    val notice =
        state
            .values<Announcement>(CampusFeed.ANNOUNCEMENTS)
            .filter {
                !it.archived &&
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
            .filter { !it.cancelled && !it.archived && it.id in tickets && it.startTime >= System.currentTimeMillis() }
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
        if(now.toLocalDate() in holidays)item {CampusCard("Campus holiday",state.values<CalendarDay>(CampusFeed.CALENDAR).filter {it.date==now.toLocalDate().toString()}.joinToString {it.title})}
        current?.let {c->item {CampusCard(c.courseName,"Until ${clockTime(c.endMinute)} · ${c.room}\n${c.faculty}","In class") {open("Timetable")}}}
        if(!state.feed(CampusFeed.TIMETABLE).loading)item {
            CampusCard(next?.entry?.courseName?:"No upcoming classes",
                next?.let { val minutes=java.time.Duration.between(now,it.date).toMinutes();"${if(it.date.toLocalDate()==now.toLocalDate()) "Starts in $minutes min" else "${it.date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }} · ${it.date.toLocalDate()}"} · ${clockTime(it.entry.startMinute)}\n${it.entry.room} · ${it.entry.faculty}" }?:"Check your weekly timetable or academic profile.","Next class") {open("Timetable")}
        }

        item {
            CampusCard(
                route?.name ?: "Choose your bus route",
                trip?.location?.let {location->
                    val stop=selectedBoardingStop(route,profile.boardingStop)
                    "${if(location.simulated) "Demo GPS · " else ""}${if(stop==null) "Choose a boarding stop to see your ETA" else routeEtaMinutes(location,route,stop,System.currentTimeMillis())?.let { "Route estimate: $it min to ${stop.name}" }?:"ETA unavailable · check route and GPS freshness"}\nUpdated ${dateTime(location.updatedAt)}"
                } ?: "Live location is unavailable. Check routes and stops.",
                "TRANSPORT",
            ) {
                open("Bus")
            }
        }

        notice?.let {
            item {
                CampusCard(it.title, it.body.take(160), it.priority.uppercase()) { open("Notice:${it.id}") }
            }
        }
        event?.let {
            item {
                CampusCard(it.title, "${dateTime(it.startTime)} | ${it.venue}", "YOUR NEXT EVENT") {
                    open("Event:${it.id}")
                }
            }
        }

        order?.let {
            item {
                CampusCard(
                    "Order ${it.token}",
                    "${money(it.totalPaise)} | Pay at counter",
                    it.status.name,
                ) {
                    open("Order:${it.id}")
                }
            }
        }
        item { TextButton(onClick = { open("Inbox") }) { Text("Open campus inbox") } }
    }
}
