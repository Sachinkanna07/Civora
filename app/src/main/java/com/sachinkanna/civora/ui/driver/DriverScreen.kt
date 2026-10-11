package com.sachinkanna.civora.ui.driver

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sachinkanna.civora.data.datasource.TripTrackingService
import com.sachinkanna.civora.data.model.*
import com.sachinkanna.civora.data.repository.*
import com.sachinkanna.civora.ui.components.*
import com.sachinkanna.civora.viewmodel.*

@Composable
fun DriverScreen(state: WorkspaceState,vm: CampusWorkspaceViewModel,logout: ()->Unit) {
    val context=LocalContext.current
    var permission by remember {mutableStateOf(TripTrackingService.canShareLocation(context))}
    var permissionMessage by remember {mutableStateOf<String?>(null)}
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {permission=TripTrackingService.canShareLocation(context);permissionMessage=if(permission)null else "Allow location and trip notifications so sharing remains visible and you can stop it."}
    val lifecycle=androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer=androidx.lifecycle.LifecycleEventObserver {_,event->if(event==androidx.lifecycle.Lifecycle.Event.ON_RESUME){permission=TripTrackingService.canShareLocation(context);if(permission)permissionMessage=null}}
        lifecycle.addObserver(observer)
        onDispose {lifecycle.removeObserver(observer)}
    }
    val locationState by TripTrackingService.status.collectAsStateWithLifecycle()
    val sharing by TripTrackingService.activeTrip.collectAsStateWithLifecycle()
    val bus=state.values<Bus>(CampusFeed.BUSES).firstOrNull()
    val trip=state.values<ActiveTrip>(CampusFeed.TRIPS).find {it.busId==bus?.id}
    val route=state.values<BusRoute>(CampusFeed.ROUTES).find {it.id==bus?.routeId}
    var confirmEnd by remember {mutableStateOf(false)}
    var issue by rememberSaveable {mutableStateOf("")};var note by rememberSaveable {mutableStateOf("")};var delay by rememberSaveable {mutableStateOf("10")}
    var requestId by rememberSaveable {mutableStateOf(java.util.UUID.randomUUID().toString())}
    val startSharing: (String)->Unit={id->try {TripTrackingService.start(context,id)}catch(_: Exception) {permissionMessage="Open this workspace and allow location before starting sharing."}}
    CampusPage("Driver workspace") {
        feedState(CampusFeed.BUSES,state,vm,"Ask campus operations to assign your bus and route.")
        item {CampusCard(bus?.name?:"No vehicle assigned",route?.name?:"No route assigned",if(trip!=null)"Trip active"else"Trip stopped")}
        item {SectionTitle("Location sharing",locationState);Text("Sharing continues during an active trip when you switch apps or lock the phone. The notification always includes Stop sharing.",style=MaterialTheme.typography.bodyMedium)}
        if(!permission)item {
            Button(onClick={
                val requested=listOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION)+if(android.os.Build.VERSION.SDK_INT>=33)listOf(Manifest.permission.POST_NOTIFICATIONS)else emptyList()
                launcher.launch(requested.toTypedArray())
            },modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text("Allow trip permissions")}
            TextButton(onClick={context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,context.packageName))}) {Text("Open Android notification settings")}
        }
        permissionMessage?.let {item {Text(it,color=MaterialTheme.colorScheme.error)}}
        bus?.let {b->item {
            if(trip==null)Button(onClick={vm.action("manageTrip",mapOf("busId" to b.id,"active" to true),"Trip started") {startSharing(b.id)}},enabled=permission&&!state.busy,modifier=Modifier.fillMaxWidth().heightIn(min=72.dp)) {Text("Start trip and share GPS")}
            else Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                if(sharing!=trip.id)Button(onClick={startSharing(trip.id)},enabled=permission&&!state.busy,modifier=Modifier.fillMaxWidth().heightIn(min=64.dp)) {Text("Resume GPS sharing")}
                else OutlinedButton(onClick={TripTrackingService.stop(context)},modifier=Modifier.fillMaxWidth().heightIn(min=64.dp)) {Text("Pause GPS sharing")}
                Button(onClick={confirmEnd=true},enabled=!state.busy,modifier=Modifier.fillMaxWidth().heightIn(min=72.dp),colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.error)) {Text("End trip")}
            }
        }}
        trip?.location?.let {location->item {CampusCard(if(location.simulated)"Demo GPS (simulated)"else"Last confirmed GPS",dateTime(location.updatedAt),if(System.currentTimeMillis()-location.updatedAt>120000)"Stale"else"Live")}}
        if(bus!=null)item {SectionTitle("Transport update","Send a delay, breakdown, disruption or all-clear to campus operations and riders.");FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("DELAY","BREAKDOWN","DISRUPTION","CLEARED").forEach {kind->FilterChip(issue==kind,{issue=kind;requestId=java.util.UUID.randomUUID().toString()},label={Text(kind.lowercase().replaceFirstChar {it.uppercase()})})}}
            if(issue.isNotBlank())Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                if(issue=="DELAY")OutlinedTextField(delay,{delay=it.filter(Char::isDigit).take(3);requestId=java.util.UUID.randomUUID().toString()},label={Text("Delay in minutes")},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(note,{note=it.take(500);requestId=java.util.UUID.randomUUID().toString()},label={Text("Update for riders")},minLines=2,modifier=Modifier.fillMaxWidth())
                Button(onClick={vm.action("reportTransportIssue",mapOf("busId" to bus.id,"kind" to issue,"note" to note,"delayMinutes" to if(issue=="DELAY")delay.toIntOrNull()else 0,"requestId" to requestId),"Transport update sent") {issue="";note="";requestId=java.util.UUID.randomUUID().toString()}},enabled=!state.busy&&(issue!="DELAY"||delay.toIntOrNull() in 0..180),modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text("Send update")}
            }
        }
        if(route?.contactPhone?.isNotBlank()==true)item {OutlinedButton(onClick={context.startActivity(android.content.Intent(android.content.Intent.ACTION_DIAL,android.net.Uri.parse("tel:${route.contactPhone}")))},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text("Call campus transport operations")}}
        items(route?.stops.orEmpty()) {CampusCard(it.name,"Route stop")}
        item {OutlinedButton(onClick={TripTrackingService.stop(context);logout()},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text("Log out")}}
    }
    if(confirmEnd&&bus!=null)ConfirmationDialog("End this trip?","GPS sharing will stop after campus services confirm the trip has ended.","End trip",{vm.action("manageTrip",mapOf("busId" to bus.id,"active" to false),"Trip ended") {TripTrackingService.stop(context)};confirmEnd=false},{confirmEnd=false})
}
