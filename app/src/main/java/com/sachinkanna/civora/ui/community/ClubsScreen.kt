package com.sachinkanna.civora.ui.community

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
fun ClubsScreen(
    state: WorkspaceState,
    vm: CampusWorkspaceViewModel,
    back: () -> Unit,
    open: (String) -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    val clubs = state.values<Club>(CampusFeed.CLUBS)
    val club = clubs.find { it.id == selected }
    CampusPage(club?.name ?: "Clubs", { if (selected != null) selected = null else back() }) {
        feedState(CampusFeed.CLUBS, state, vm, "No clubs published yet.")
        if (club != null) {
            item {
                Text(club.description)
                Text(club.category, style = MaterialTheme.typography.labelLarge)
                Button(onClick = { vm.follow(club.id) }, enabled = !state.busy) {
                    Text(
                        if (club.id in state.values<String>(CampusFeed.FOLLOWS)) "Unfollow"
                        else "Follow club"
                    )
                }
            }
            val events =
                state.values<CampusEvent>(CampusFeed.EVENTS).filter { it.clubId == club.id }
            item {
                Text("Upcoming club events", style = MaterialTheme.typography.titleMedium)
                if (events.isEmpty()) Text("No upcoming events.")
            }
            items(events, key = { it.id }) {
                CampusCard(it.title, dateTime(it.startTime)) { open("Event:${it.id}") }
            }
        } else {
            item {
                OutlinedTextField(
                    search,
                    { search = it },
                    label = { Text("Search clubs") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            items(
                clubs.filter { "${it.name} ${it.category}".contains(search, true) },
                key = { it.id },
            ) {
                CampusCard(it.name, it.description.take(140), it.category) { selected = it.id }
            }
        }
    }
}
