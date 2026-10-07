package com.sachinkanna.civora.ui.discover

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
fun DiscoverScreen(state: WorkspaceState, vm: CampusWorkspaceViewModel, open: (String) -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    val categories =
        listOf("Events", "Clubs", "Food", "Notices", "Campus services", "Bus", "Timetable")
    CampusPage("Discover") {
        item {
            OutlinedTextField(
                search,
                { search = it },
                label = { Text("Search campus") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        items(categories.filter { it.contains(search, true) }) {
            CampusCard(
                it,
                when (it) {
                    "Events" -> "Workshops, festivals and your registrations"
                    "Clubs" -> "Find and follow your campus communities"
                    "Food" -> "Canteens, menus and pickup orders"
                    "Notices" -> "Academic and department announcements"
                    "Campus services" -> "Find campus places and directions"
                    "Bus" -> "Routes, stops and latest trip location"
                    else -> "Today's classes and weekly schedule"
                },
            ) {
                open(it)
            }
        }
        if (search.isNotBlank()) {
            items(
                state.values<CampusEvent>(CampusFeed.EVENTS).filter {
                    "${it.title} ${it.description} ${it.category}".contains(search, true)
                },
                key = { "event${it.id}" },
            ) {
                CampusCard(it.title, "${dateTime(it.startTime)} | ${it.venue}", "EVENT") {
                    open("Event:${it.id}")
                }
            }
            items(
                state.values<Club>(CampusFeed.CLUBS).filter {
                    "${it.name} ${it.category} ${it.description}".contains(search, true)
                },
                key = { "club${it.id}" },
            ) {
                CampusCard(it.name, it.category, "CLUB") { open("Clubs") }
            }
            items(
                state.values<MenuItem>(CampusFeed.MENU).filter { it.name.contains(search, true) },
                key = { "menu${it.id}" },
            ) {
                CampusCard(it.name, money(it.pricePaise), "FOOD") { open("Food") }
            }
            items(
                state.values<Announcement>(CampusFeed.ANNOUNCEMENTS).filter {
                    "${it.title} ${it.body}".contains(search, true)
                },
                key = { "notice${it.id}" },
            ) {
                CampusCard(it.title, it.category, "NOTICE") { open("Notice:${it.id}") }
            }
            item {
                Text(
                    "Search covers the most recent loaded campus items.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        feedState(CampusFeed.EVENTS, state, vm, "No upcoming events published.")
    }
}
