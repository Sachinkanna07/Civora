package com.sachinkanna.civora.ui.transport

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.sachinkanna.civora.data.model.*
import com.sachinkanna.civora.data.repository.*
import com.sachinkanna.civora.ui.components.*
import com.sachinkanna.civora.viewmodel.*

@Composable
fun TransportScreen(
    profile: UserProfile,
    state: WorkspaceState,
    vm: CampusWorkspaceViewModel,
    back: () -> Unit,
) {
    var routeId by rememberSaveable { mutableStateOf(profile.routeId) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    val routes = state.values<BusRoute>(CampusFeed.ROUTES)
    val route = routes.find { it.id == routeId } ?: routes.firstOrNull()
    val trip = state.values<ActiveTrip>(CampusFeed.TRIPS).find { it.routeId == route?.id }
    val context = LocalContext.current
    CampusPage("Campus transport", back) {
        feedState(CampusFeed.ROUTES, state, vm, "No routes published yet.")
        item {
            FlowRow {
                routes.forEach { r ->
                    FilterChip(route?.id == r.id, { routeId = r.id }, label = { Text(r.name) })
                }
            }
        }
        item {
            CampusCard(
                route?.name ?: "Routes unavailable",
                if (trip == null) "No active trip on this route."
                else
                    trip.location?.let {
                        "${if(isLocationFresh(it,now))"Live location" else "Stale location"}\nLast updated ${dateTime(it.updatedAt)}"
                    } ?: "Driver has started the trip. Waiting for location.",
                "TRIP STATUS",
            )
        }
        feedState(CampusFeed.TRIPS, state, vm, "No active trips.")
        trip?.location?.let { location ->
            item {
                OutlinedButton(
                    onClick = {
                        openMap(context, location.latitude, location.longitude, "Campus bus")
                    }
                ) {
                    Text("Open latest bus location")
                }
            }
        }
        items(route?.stops.orEmpty()) { stop ->
            CampusCard(
                stop.name,
                etaMinutes(trip?.location, stop, now)?.let {
                    "Approx. $it min | distance estimate at 20 km/h"
                } ?: "ETA unavailable",
                "BUS STOP",
            )
        }
        item {
            Text(
                "ETAs are rough distance estimates and do not include road traffic or stop delays.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
