package com.sachinkanna.civora.ui.food

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.sachinkanna.civora.data.model.*
import com.sachinkanna.civora.data.repository.*
import com.sachinkanna.civora.ui.components.*
import com.sachinkanna.civora.viewmodel.*

@Composable
fun KitchenScreen(state: WorkspaceState,vm: CampusWorkspaceViewModel,onLogout: ()->Unit) {
    var section by rememberSaveable {mutableStateOf("Orders")}
    var queue by rememberSaveable {mutableStateOf("New")}
    var kitchenId by rememberSaveable {mutableStateOf("")}
    val kitchens=state.values<Canteen>(CampusFeed.CANTEENS)
    val kitchen=kitchens.find {it.id==kitchenId}?:kitchens.firstOrNull()
    val orders=state.values<FoodOrder>(CampusFeed.ORDERS).filter {it.canteenId==kitchen?.id}
    val context=LocalContext.current
    val uid=com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val preferences=remember(uid) {context.getSharedPreferences("kitchen_$uid",android.content.Context.MODE_PRIVATE)}
    var alerts by rememberSaveable(uid) {mutableStateOf(preferences.getBoolean("alerts",true))}
    var latest by remember(uid) {mutableLongStateOf(0)}
    var initialized by remember(uid) {mutableStateOf(false)}
    LaunchedEffect(orders,state.feed(CampusFeed.ORDERS).loading) {
        if(!state.feed(CampusFeed.ORDERS).loading) {
            val newest=orders.maxOfOrNull {it.createdAt}?:0
            if(initialized&&alerts&&orders.any {it.status==FoodOrderStatus.PLACED&&it.createdAt>latest}) {
                val tone=android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION,70)
                try {tone.startTone(android.media.ToneGenerator.TONE_PROP_BEEP,300);kotlinx.coroutines.delay(350)} finally {tone.release()}
            }
            latest=maxOf(latest,newest);initialized=true
        }
    }
    CampusPage("Kitchen workspace") {
        feedState(CampusFeed.CANTEENS,state,vm,"Campus operations must assign a kitchen to your account.")
        if(kitchen!=null) {
            item {SelectField("Kitchen",kitchen.id,kitchens.map {Choice(it.id,it.name)},{kitchenId=it})}
            item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("Orders","Menu","Settings").forEach {name->FilterChip(section==name,{section=name},label={Text(name)})}}}
            if(section=="Orders") {
                item {SectionTitle("Pickup queue","Oldest orders appear first. Advance an order once the kitchen completes each step.")}
                item {FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("New","Preparing","Ready","Completed").forEach {name->FilterChip(queue==name,{queue=name},label={Text("$name (${kitchenQueue(orders,name).size})")})}}}
                feedState(CampusFeed.ORDERS,state,vm,"No pickup orders yet.")
                val visible=kitchenQueue(orders,queue)
                if(visible.isEmpty()&&!state.feed(CampusFeed.ORDERS).loading)item {EmptyState("No ${queue.lowercase()} orders","Your queue updates when students place orders or the kitchen changes their status.")}
                items(visible,key={it.id}) {o->OrderCard(o,state,vm,true)}
            } else if(section=="Menu") {
                feedState(CampusFeed.MENU,state,vm,"Add the first menu item below.")
                items(state.values<MenuItem>(CampusFeed.MENU).filter {it.canteenId==kitchen.id},key={it.id}) {m->CampusCard(m.name,"${m.category} · ${money(m.pricePaise)}",if(m.available)"Available"else"Sold out")
                    OutlinedButton(onClick={vm.action("setMenuAvailability",mapOf("menuItemId" to m.id,"available" to !m.available),"Availability updated")},enabled=!state.busy,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) {Text(if(m.available)"Mark ${m.name} sold out"else"Make ${m.name} available")}
                }
                item {MenuItemEditor(kitchen.id,state,vm)}
            } else {
                item {KitchenSettings(kitchen,state,vm)}
                item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Column(Modifier.weight(1f)) {Text("New order sound",style=MaterialTheme.typography.titleMedium);Text("Sounds while this kitchen workspace is open.",style=MaterialTheme.typography.bodySmall)};Switch(alerts,{alerts=it;preferences.edit().putBoolean("alerts",it).apply()})}}
            }
        }
        item {TextButton(onClick=onLogout) {Text("Log out")}}
    }
}

@Composable
private fun KitchenSettings(kitchen: Canteen,state: WorkspaceState,vm: CampusWorkspaceViewModel) {
    var prep by rememberSaveable(kitchen.id,kitchen.preparationMinutes) {mutableStateOf(kitchen.preparationMinutes?.toString().orEmpty())}
    fun update(open: Boolean=kitchen.open,paused: Boolean=kitchen.paused) {vm.action("setCanteenOperations",mapOf("canteenId" to kitchen.id,"open" to open,"paused" to paused,"preparationMinutes" to if(prep.isBlank())null else prep.toIntOrNull()),"Kitchen settings updated")}
    Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
        StatusPill(if(!kitchen.open)"Closed"else if(kitchen.paused)"Paused"else"Open")
        Text("Closing or pausing stops new orders. Finish existing orders from the queue.")
        OutlinedTextField(prep,{prep=it.filter(Char::isDigit).take(3)},label={Text("Preparation estimate in minutes (optional)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())
        val valid=prep.isBlank()||prep.toIntOrNull() in 1..180
        Button(onClick={update()},enabled=valid&&!state.busy,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) {Text("Save preparation estimate")}
        OutlinedButton(onClick={update(open=!kitchen.open)},enabled=valid&&!state.busy,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) {Text(if(kitchen.open)"Close kitchen"else"Open kitchen")}
        OutlinedButton(onClick={update(paused=!kitchen.paused)},enabled=valid&&!state.busy&&kitchen.open,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) {Text(if(kitchen.paused)"Resume new orders"else"Pause new orders")}
    }
}

@Composable
private fun MenuItemEditor(kitchenId: String,state: WorkspaceState,vm: CampusWorkspaceViewModel) {
    var name by rememberSaveable(kitchenId) {mutableStateOf("")};var category by rememberSaveable(kitchenId) {mutableStateOf("Meals")};var price by rememberSaveable(kitchenId) {mutableStateOf("")}
    var requestId by rememberSaveable(kitchenId) {mutableStateOf(java.util.UUID.randomUUID().toString())}
    val paise=runCatching {java.math.BigDecimal(price).movePointRight(2).longValueExact()}.getOrNull()
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        SectionTitle("Add menu item")
        OutlinedTextField(name,{name=it.take(100)},label={Text("Item name")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(category,{category=it.take(50)},label={Text("Category")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(price,{price=it.take(8)},label={Text("Price in INR")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())
        Button(onClick={vm.action("saveMenuItem",mapOf("id" to requestId,"canteenId" to kitchenId,"name" to name,"category" to category,"pricePaise" to paise,"available" to true),"Menu item added") {name="";price="";requestId=java.util.UUID.randomUUID().toString()}},enabled=!state.busy&&name.isNotBlank()&&category.isNotBlank()&&paise!=null&&paise in 0..100000,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) {Text("Add item")}
    }
}
