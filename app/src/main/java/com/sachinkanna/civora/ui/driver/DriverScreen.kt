package com.sachinkanna.civora.ui.driver

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sachinkanna.civora.data.datasource.DriverLocationSource
import com.sachinkanna.civora.data.model.*
import com.sachinkanna.civora.data.repository.*
import com.sachinkanna.civora.ui.components.*
import com.sachinkanna.civora.viewmodel.*

@Composable
fun DriverScreen(state: WorkspaceState, vm: CampusWorkspaceViewModel, logout: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var permission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var locationState by remember { mutableStateOf("Location sharing paused") }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permission = it.values.any { value -> value }
        }
    val bus = state.values<Bus>(CampusFeed.BUSES).firstOrNull()
    val trip = state.values<ActiveTrip>(CampusFeed.TRIPS).find { it.busId == bus?.id }
    val route = state.values<BusRoute>(CampusFeed.ROUTES).find { it.id == bus?.routeId }
    DisposableEffect(trip?.id, permission, owner) {
        if (trip == null) locationState = "Location sharing paused"
        else if (!permission) locationState = "Location permission needed"
        val source =
            if (trip != null && permission)
                DriverLocationSource(context, trip.id) { locationState = it }
            else null
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) source?.start()
            else if (event == Lifecycle.Event.ON_PAUSE) {
                source?.stop()
                locationState = "Location sharing paused"
            }
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) source?.start()
        onDispose {
            source?.stop()
            owner.lifecycle.removeObserver(observer)
        }
    }
    CampusPage("Driver workspace") {
        feedState(
            CampusFeed.BUSES,
            state,
            vm,
            "No bus assigned. Ask campus operations to assign your bus and route.",
        )
        item {
            CampusCard(
                bus?.name ?: "Assignment unavailable",
                route?.name ?: "Route unavailable",
                if (trip != null) "TRIP ACTIVE" else "TRIP STOPPED",
            )
        }
        item {
            Button(
                onClick = {
                    launcher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Text(if (permission) "Location permission granted" else "Allow location sharing")
            }
            Text(locationState)
            Text(
                "GPS is shared every 15 seconds while this screen is visible. Leaving the app pauses sharing.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        bus?.let { b ->
            item {
                Button(
                    onClick = {
                        vm.action("manageTrip", mapOf("busId" to b.id, "active" to (trip == null)))
                    },
                    enabled = !state.busy && (trip != null || permission),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                ) {
                    Text(if (trip == null) "Start trip" else "End trip")
                }
            }
        }
        trip?.location?.let { item { Text("Last transmitted: ${dateTime(it.updatedAt)}") } }
        items(route?.stops.orEmpty()) { CampusCard(it.name, "Route stop") }
        item {
            OutlinedButton(onClick = logout, modifier = Modifier.fillMaxWidth()) { Text("Log out") }
        }
    }
}
