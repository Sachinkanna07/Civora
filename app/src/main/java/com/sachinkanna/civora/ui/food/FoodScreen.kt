package com.sachinkanna.civora.ui.food

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
fun FoodScreen(state: WorkspaceState,vm: CampusWorkspaceViewModel,back: ()->Unit,orderId: String?=null,vendor: Boolean=false) {
    if(vendor) { KitchenScreen(state,vm,back);return }
    var stage by rememberSaveable { mutableStateOf(if(orderId==null) "Canteens" else "Tracking") }
    var canteenId by rememberSaveable { mutableStateOf("") }
    var trackingId by rememberSaveable { mutableStateOf(orderId.orEmpty()) }
    var replaceWith by remember { mutableStateOf<Canteen?>(null) }
    var cancelling by remember { mutableStateOf<FoodOrder?>(null) }
    val canteens=state.values<Canteen>(CampusFeed.CANTEENS)
    val canteen=canteens.find {it.id==canteenId}
    val cartKitchen=canteens.find {it.id==state.cart.firstOrNull()?.item?.canteenId}
    val orders=state.values<FoodOrder>(CampusFeed.ORDERS)
    LaunchedEffect(state.lastOrderId) {state.lastOrderId?.let {trackingId=it;stage="Tracking"}}
    val goBack: ()->Unit={when(stage) {"Menu","History","Tracking"->stage="Canteens";"Cart"->stage=if(canteenId.isBlank())"Canteens"else"Menu";"Checkout"->stage="Cart";else->back()}}
    CampusPage(when(stage) {"Menu"->canteen?.name?:"Menu";"Cart"->"Your cart";"Checkout"->"Review order";"Tracking"->"Order tracking";"History"->"Order history";else->"Campus food"},goBack) {
        when(stage) {
            "Canteens"-> {
                item { SectionTitle("Choose a canteen","Order for pickup. Pay at the counter.") }
                if(state.cart.isNotEmpty())item { CampusCard("Continue your cart","${state.cart.sumOf {it.quantity}} items · ${money(cartTotal(state.cart))}") {stage="Cart"} }
                item {TextButton(onClick={stage="History"}) {Text("View my orders")}}
                feedState(CampusFeed.CANTEENS,state,vm,"Campus canteens will appear here when published.")
                items(canteens,key={it.id}) {c->CampusCard(c.name,c.preparationMinutes?.let {"Kitchen estimate: $it min"}?:"Preparation estimate not provided",if(!c.open)"Closed"else if(c.paused)"Paused"else"Open") {
                    if(state.cart.isNotEmpty()&&state.cart.first().item.canteenId!=c.id)replaceWith=c else {canteenId=c.id;stage="Menu"}
                }}
            }
            "Menu"-> {
                item {CampusCard(canteen?.name?:"Canteen unavailable",canteen?.preparationMinutes?.let {"Kitchen estimate: $it min · Pay at counter"}?:"Pay at counter",if(canteen?.open!=true)"Closed"else if(canteen.paused)"Paused"else"Open")}
                if(state.cart.isNotEmpty())item {Button(onClick={stage="Cart"},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text("View cart · ${money(cartTotal(state.cart))}")}}
                feedState(CampusFeed.MENU,state,vm,"This menu has not been published yet.")
                val menu=state.values<MenuItem>(CampusFeed.MENU).filter {it.canteenId==canteenId}.sortedWith(compareBy({it.category},{it.name}))
                items(menu,key={it.id}) {m->OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(m.name,style=MaterialTheme.typography.titleMedium);Text("${m.category} · ${money(m.pricePaise)}")
                    if(m.description.isNotBlank())Text(m.description)
                    if(!m.available)StatusPill("Sold out")
                    val quantity=state.cart.find {it.item.id==m.id}?.quantity?:0
                    Button(onClick={vm.add(m,1)},enabled=m.available&&canteen?.open==true&&canteen.paused.not()&&!state.busy,modifier=Modifier.heightIn(min=48.dp)) {Text(if(quantity>0)"Add another · $quantity in cart"else"Add to cart")}
                }}}
            }
            "Cart","Checkout"-> {
                item {SectionTitle(cartKitchen?.name?:"Your canteen",if(stage=="Checkout")"Confirm pickup details before ordering."else"Your cart is saved on this device.")}
                if(state.cart.isEmpty())item {EmptyState("Your cart is empty","Choose a canteen to add your first item.",action="Browse canteens",onAction={stage="Canteens"})}
                items(state.cart,key={it.item.id}) {c->OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp)) {
                    Text(c.item.name,style=MaterialTheme.typography.titleMedium);Text("${money(c.item.pricePaise)} · ${c.quantity} · ${money(c.item.pricePaise*c.quantity)}")
                    if(!c.item.available)Text("Unavailable. Remove this item to continue.",color=MaterialTheme.colorScheme.error)
                    if(stage=="Cart")Row {OutlinedButton(onClick={vm.add(c.item,-1)},modifier=Modifier.heightIn(min=48.dp)) {Text("Remove one")};Spacer(Modifier.width(8.dp));OutlinedButton(onClick={vm.add(c.item,1)},enabled=c.item.available&&c.quantity<20,modifier=Modifier.heightIn(min=48.dp)) {Text("Add one")}}
                }}}
                if(state.cart.isNotEmpty())item {Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text("Total ${money(cartTotal(state.cart))}",style=MaterialTheme.typography.titleLarge)
                    if(stage=="Checkout") {Text("Payment: pay at counter. This app does not collect payment.");Text(cartKitchen?.preparationMinutes?.let {"Kitchen estimate: $it minutes. Actual readiness appears in tracking."}?:"The kitchen has not provided a preparation estimate.")}
                    val available=cartKitchen?.open==true&&cartKitchen.paused.not()&&state.cart.all {it.item.available}
                    if(!available)Text("Ordering is paused, closed, or an item is unavailable. Keep your cart and try later.",color=MaterialTheme.colorScheme.error)
                    Button(onClick={if(stage=="Cart")stage="Checkout"else vm.placeOrder()},enabled=available&&!state.busy,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text(if(state.busy)"Confirming order?"else if(stage=="Cart")"Review order"else"Confirm pickup order")}
                    if(stage=="Cart")TextButton(onClick={vm.clearCart()}) {Text("Clear cart")}
                }}
            }
            else-> {
                feedState(CampusFeed.ORDERS,state,vm,"Your orders and pickup tokens will appear here.")
                val visible=if(stage=="Tracking")orders.filter {it.id==trackingId}else orders.sortedByDescending {it.createdAt}
                if(stage=="Tracking"&&visible.isEmpty()&&!state.feed(CampusFeed.ORDERS).loading)item {EmptyState("Waiting for order confirmation","Your order may still be syncing. Reopen order history to check its latest status.")}
                items(visible,key={it.id}) {o->OrderCard(o,state,vm,false,{cancelling=o})}
            }
        }
    }
    replaceWith?.let {c->ConfirmationDialog("Switch canteen?","Your current cart will be cleared before opening ${c.name}.","Switch canteen",{vm.clearCart();canteenId=c.id;stage="Menu";replaceWith=null},{replaceWith=null})}
    cancelling?.let {o->ConfirmationDialog("Cancel pickup order?","Token ${o.token} will be cancelled. The kitchen can no longer prepare it.","Cancel order",{vm.action("updateFoodOrder",mapOf("orderId" to o.id,"status" to "CANCELLED"),"Order cancelled");cancelling=null},{cancelling=null})}
}

@Composable
internal fun OrderCard(order: FoodOrder,state: WorkspaceState,vm: CampusWorkspaceViewModel,kitchen: Boolean,onCancel: ()->Unit={}) {
    OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("Token ${order.token}",style=if(kitchen)MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineSmall)
        StatusPill(order.status.name)
        Text("${order.canteenName} · ${dateTime(order.createdAt)}",style=MaterialTheme.typography.bodySmall)
        order.items.forEach {Text("${it.quantity} · ${it.name}",style=MaterialTheme.typography.titleMedium)}
        Text("${money(order.totalPaise)} · Pay at counter")
        if(!kitchen)Text(when(order.status) {FoodOrderStatus.PLACED->"Waiting for the kitchen to accept your order.";FoodOrderStatus.ACCEPTED->"Accepted by the kitchen.";FoodOrderStatus.PREPARING->"The kitchen is preparing your food.";FoodOrderStatus.READY->"Ready for pickup. Show this token at the counter.";FoodOrderStatus.COMPLETED->"Pickup complete.";FoodOrderStatus.CANCELLED->"This order was cancelled.";else->"The kitchen could not fulfil this order."})
        if(kitchen)nextKitchenStatus(order.status)?.let {next->Button(onClick={vm.action("updateFoodOrder",mapOf("orderId" to order.id,"status" to next.name),"Order updated")},enabled=!state.busy,modifier=Modifier.fillMaxWidth().heightIn(min=60.dp)) {Text(when(next) {FoodOrderStatus.ACCEPTED->"Accept order";FoodOrderStatus.PREPARING->"Start preparing";FoodOrderStatus.READY->"Mark ready";else->"Confirm pickup"})}}
        else if(order.status==FoodOrderStatus.PLACED)OutlinedButton(onClick=onCancel,enabled=!state.busy,modifier=Modifier.heightIn(min=48.dp)) {Text("Cancel order")}
    }}
}
