package com.sachinkanna.civora.ui.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    inbox: Boolean = false,
    back: (() -> Unit)? = null,
) {
    val feed = if (inbox) CampusFeed.INBOX else CampusFeed.ACTIVITIES
    CampusPage(if (inbox) "Inbox" else "Activity", back) {
        feedState(
            feed,
            state,
            vm,
            if (inbox) "You're all caught up."
            else "Your registrations, orders and reports will appear here as you use Civora.",
        )
        if (inbox)
            items(state.values<InboxItem>(feed), key = { it.id }) { item ->
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
        Destination.FOOD -> "Order:$id"
        Destination.REPORT -> "Report:$id"
        Destination.ANNOUNCEMENT -> "Notice:$id"
        Destination.TRANSPORT -> "Bus"
        null -> null
    }
