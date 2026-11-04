package com.sachinkanna.civora.ui.mess

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
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
fun MessScreen(profile: UserProfile,state: WorkspaceState,vm: CampusWorkspaceViewModel,back: ()->Unit) {
    val operator=profile.role==UserRole.MESS
    val halls=state.values<MessHall>(CampusFeed.MESS_HALLS)
    val periods=state.values<MealPeriod>(CampusFeed.MEAL_PERIODS)
    val history=state.values<MealCheckIn>(CampusFeed.MEAL_CHECKINS)
    var hallId by rememberSaveable {mutableStateOf("")};var periodId by rememberSaveable {mutableStateOf("")}
    val hall=halls.find {it.id==hallId}?:halls.firstOrNull()
    val meals=periods.filter {it.messId==hall?.id}.sortedByDescending {it.opensAt}
    var scanning by remember {mutableStateOf(false)};var ticket by rememberSaveable {mutableStateOf("")}
    var pass by remember {mutableStateOf("")};var expires by remember {mutableLongStateOf(0)};var count by remember {mutableStateOf<Long?>(null)}
    var now by remember {mutableLongStateOf(System.currentTimeMillis())}
    LaunchedEffect(Unit) {while(true) {now=System.currentTimeMillis();kotlinx.coroutines.delay(1000)}}
    LaunchedEffect(state.lastResult) {
        if(state.lastAction=="getMealPass") {pass=state.lastResult["payload"] as? String?:"";expires=(state.lastResult["expiresAt"] as? Number)?.toLong()?:0}
        if(state.lastAction=="getMessSummary"&&state.lastResult["periodId"]==periodId)count=(state.lastResult["count"] as? Number)?.toLong()
        if(state.lastAction=="checkInMeal")ticket=""
    }
    LaunchedEffect(periodId) {count=null}
    LaunchedEffect(state.busy,state.lastAction) {if(operator&&!state.busy&&state.lastAction=="checkInMeal"&&periodId.isNotBlank())vm.action("getMessSummary",mapOf("periodId" to periodId),"Meal count refreshed")}
    if(scanning)TicketScanner("civora-meal",{ticket=it;scanning=false},{scanning=false})
    CampusPage(if(operator)"Mess check-in"else"Mess meals",if(operator)null else back) {
        feedState(CampusFeed.MESS_HALLS,state,vm,"No mess hall has been configured.")
        if(hall!=null) {
            item {SelectField("Mess hall",hall.id,halls.map {Choice(it.id,it.name)},{hallId=it;periodId="";pass=""});StatusPill(if(hall.open)"Open"else"Closed")}
            if(operator) {
                item {SelectField("Meal period",periodId,meals.map {Choice(it.id,it.label,"${dateTime(it.opensAt)} · ${dateTime(it.closesAt)}")},{periodId=it})}
                item {Button(onClick={vm.action("getMessSummary",mapOf("periodId" to periodId),"Meal count refreshed")},enabled=periodId.isNotBlank()&&!state.busy) {Text("Refresh meal count")};count?.let {Text("$it checked-in meals",style=MaterialTheme.typography.headlineSmall)}}
                item {Button(onClick={scanning=true},enabled=periodId.isNotBlank()&&!state.busy,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text("Scan student meal pass")}}
                item {OutlinedTextField(ticket,{ticket=it.take(500)},label={Text("Meal pass code (fallback)")},modifier=Modifier.fillMaxWidth());val parsed=parseTicketPayload(ticket,"civora-meal")
                    Button(onClick={vm.action("checkInMeal",mapOf("periodId" to periodId,"userId" to parsed!!.first,"token" to parsed.second),"Meal checked in")},enabled=parsed!=null&&periodId.isNotBlank()&&!state.busy,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text("Validate and check in")}}
            } else {
                val entitled=state.values<MealEntitlement>(CampusFeed.ENTITLEMENTS).any {it.messId==hall.id&&it.active}
                item {CampusCard("Meal entitlement",if(entitled)"Your meal entitlement is active."else"You do not have an active entitlement here. Contact campus operations.")}
                item {Button(onClick={vm.action("getMealPass",mapOf("messId" to hall.id),"Meal pass ready")},enabled=entitled&&hall.open&&!state.busy,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text("Show meal pass")}}
                if(pass.isNotBlank())item {if(now<expires) {PassQr(pass,"Single-use meal check-in pass");Text("Valid for ${(expires-now)/1000} seconds. Show this to the mess operator.")}else EmptyState("Meal pass expired","Generate a fresh pass when you reach the mess counter.")}
                items(meals,key={it.id}) {m->CampusCard(m.label,"${dateTime(m.opensAt)} · ${dateTime(m.closesAt)}",if(now in m.opensAt..m.closesAt)"Open"else"Closed")}
            }
        }
        item {SectionTitle(if(operator)"Recent check-ins"else"My meal history")}
        feedState(CampusFeed.MEAL_CHECKINS,state,vm,"Completed meal check-ins appear here.")
        items(history.take(50),key={it.id}) {m->CampusCard(m.label,dateTime(m.createdAt),"Checked in")}
        if(operator)item {TextButton(onClick=back) {Text("Log out")}}
    }
}
