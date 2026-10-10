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
    var stopName by rememberSaveable {mutableStateOf(profile.boardingStop)}
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    val routes = state.values<BusRoute>(CampusFeed.ROUTES)
    val route = routes.find { it.id == routeId }
    val trip = state.values<ActiveTrip>(CampusFeed.TRIPS).find { it.routeId == route?.id }
    val context = LocalContext.current
    CampusPage("Campus transport", back) {
        feedState(CampusFeed.ROUTES, state, vm, "No routes published yet.")
        item {
            FlowRow {
                routes.forEach { r ->
                    FilterChip(route?.id == r.id, { routeId = r.id; stopName="" }, label = { Text(r.name) })
                }
            }
        }
        item {
            CampusCard(
                route?.name ?: "Routes unavailable",
                if (trip == null) "No active trip on this route."
                else
                    trip.location?.let {
                        "${if(it.simulated) "Demo GPS (simulated)" else if(isLocationFresh(it,now))"Live location" else "Stale location"}\nLast updated ${dateTime(it.updatedAt)}"
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
        route?.let {r->
            item {SelectField("Boarding stop",stopName,r.stops.map {Choice(it.name,it.name)},{stopName=it})}
            val stop=selectedBoardingStop(r,stopName)
            item {CampusCard(stop?.name?:"Choose your boarding stop",stop?.let {routeEtaMinutes(trip?.location,r,it,now)?.let {eta->"Route estimate: $eta minutes to your stop"}?:"ETA unavailable · GPS stale, off route, or stop already passed"}?:"Select the stop where you board this route.")}
            if(profile.role==UserRole.STUDENT)item {Button(onClick={vm.saveProfile(mapOf("routeId" to r.id,"boardingStop" to stopName)) {}},enabled=stop!=null&&!state.busy) {Text("Save preferred route and stop")}}
            items(r.stops,key={it.name}) {s->CampusCard(s.name,if(s.name==stopName)"Your boarding stop"else"Route stop") {stopName=s.name}}
            items(state.values<TransportUpdate>(CampusFeed.TRANSPORT_UPDATES).filter {it.routeId==r.id}.take(5),key={it.id}) {u->CampusCard(u.busName,"${u.note}${if(u.delayMinutes>0) " · ${u.delayMinutes} min delay" else ""}\n${dateTime(u.createdAt)}",u.kind)}
        }
        item {
            Text(
                "Estimates follow the ordered stops at an assumed 20 km/h with one minute per intermediate stop. Road geometry, traffic and actual dwell times can change arrivals.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
