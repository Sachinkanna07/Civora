package com.sachinkanna.civora.ui.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sachinkanna.civora.data.model.*
import com.sachinkanna.civora.data.repository.*
import com.sachinkanna.civora.ui.components.*
import com.sachinkanna.civora.viewmodel.*

@Composable
fun ActivityScreen(
    state: WorkspaceState,
    vm: CampusWorkspaceViewModel,
    open: (String) -> Unit,
    inbox: Boolean = true,
    back: (() -> Unit)? = null,
) {
    var showInbox by rememberSaveable {mutableStateOf(inbox)}
    var category by rememberSaveable {mutableStateOf<String?>(null)}
    val feed = if (showInbox) CampusFeed.INBOX else CampusFeed.ACTIVITIES
    CampusPage("Campus inbox", back) {
        item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {FilterChip(showInbox,{showInbox=true},label={Text("Notifications")});FilterChip(!showInbox,{showInbox=false},label={Text("My history")})}}
        if(showInbox)item {FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {FilterChip(category==null,{category=null},label={Text("All")});NotificationCategory.entries.forEach {c->FilterChip(category==c.name,{category=c.name},label={Text(c.name.lowercase().replaceFirstChar {it.uppercase()})})}}}
        feedState(
            feed,
            state,
            vm,
            if (showInbox) "You're all caught up."
            else "Your registrations, orders and reports will appear here as you use Civora.",
        )
        if (showInbox)
            items(state.values<InboxItem>(feed).filter {category==null||it.category.name==category}, key = { it.id }) { item ->
                CampusCard(
                    item.title,
                    "${item.body}\n${dateTime(item.createdAt)}",
                    if (item.read) item.category.name else "UNREAD | ${item.category}",
                ) {
                    vm.read(item.id)
                    destinationRoute(item.destination, item.entityId)?.let(open)
                }
            }
        else
            items(state.values<CampusActivity>(feed), key = { it.id }) { item ->
                val icon =
                    when (item.type) {
                        ActivityType.FOOD_ORDER -> Icons.Default.Restaurant
                        ActivityType.REPORT -> Icons.Default.Build
                        ActivityType.NOTICE -> Icons.Default.Notifications
                        else -> Icons.Default.Event
                    }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(icon, item.type.name, Modifier.padding(top = 18.dp))
                    Box(Modifier.weight(1f)) {
                        CampusCard(
                            item.title,
                            "${item.body}\n${dateTime(item.createdAt)}",
                            item.status,
                        ) {
                            destinationRoute(item.destination, item.entityId)?.let(open)
                        }
                    }
                }
            }
    }
}

fun destinationRoute(destination: Destination?, id: String): String? =
    when (destination) {
        Destination.EVENT -> "Event:$id"
        Destination.FOOD -> if(id.isBlank())"Mess"else"Order:$id"
        Destination.REPORT -> "Report:$id"
        Destination.ANNOUNCEMENT -> "Notice:$id"
        Destination.TRANSPORT -> "Bus"
        Destination.TIMETABLE -> "Timetable"
        null -> null
    }
