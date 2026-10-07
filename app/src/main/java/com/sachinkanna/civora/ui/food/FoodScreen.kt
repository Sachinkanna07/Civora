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
fun FoodScreen(
    state: WorkspaceState,
    vm: CampusWorkspaceViewModel,
    back: () -> Unit,
    orderId: String? = null,
    vendor: Boolean = false,
) {
    var statusFilter by rememberSaveable { mutableStateOf<FoodOrderStatus?>(null) }
    var canteenId by rememberSaveable { mutableStateOf<String?>(null) }
    var requestId by rememberSaveable { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    val orders = state.values<FoodOrder>(CampusFeed.ORDERS)
    val canteens = state.values<Canteen>(CampusFeed.CANTEENS)
    val canteen = canteens.find { it.id == canteenId } ?: canteens.firstOrNull()
    CampusPage(if (vendor) "Canteen operations" else "Food & orders", back) {
        if (vendor)
            item {
                Text(
                    "${orders.count {it.status.active}} open | ${orders.count {it.status==FoodOrderStatus.PREPARING}} preparing | ${orders.count {it.status==FoodOrderStatus.READY}} ready",
                    style = MaterialTheme.typography.titleMedium,
                )
                FlowRow {
                    FilterChip(
                        statusFilter == null,
                        { statusFilter = null },
                        label = { Text("All orders") },
                    )
                    FoodOrderStatus.entries.forEach { status ->
                        FilterChip(
                            statusFilter == status,
                            { statusFilter = status },
                            label = { Text(status.name.lowercase()) },
                        )
                    }
                }
            }
        feedState(CampusFeed.CANTEENS, state, vm, "No canteens available.")
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                canteens.forEach { c ->
                    FilterChip(
                        c.id == canteen?.id,
                        { canteenId = c.id },
                        label = { Text("${c.name}${if(c.open) "" else " | Closed"}") },
                    )
                }
            }
        }
        if (!vendor && state.cart.isNotEmpty()) {
            item { Text("Your cart", style = MaterialTheme.typography.titleLarge) }
            items(state.cart, key = { it.item.id }) { cart ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${cart.item.name} | ${cart.quantity}", Modifier.weight(1f))
                    TextButton(onClick = { vm.add(cart.item, -1) }) { Text("-") }
                    TextButton(onClick = { vm.add(cart.item, 1) }) { Text("+") }
                }
            }
            item {
                Text("Total ${money(cartTotal(state.cart))} | Pay at counter")
                Button(
                    onClick = { vm.placeOrder(requestId) },
                    enabled =
                        !state.busy &&
                            canteens.any { it.id == state.cart.first().item.canteenId && it.open },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Place order")
                }
                TextButton(
                    onClick = {
                        vm.clearCart()
                        requestId = java.util.UUID.randomUUID().toString()
                    }
                ) {
                    Text("Clear cart")
                }
            }
        }
        feedState(CampusFeed.MENU, state, vm, "Menu has not been published.")
        items(
            state.values<MenuItem>(CampusFeed.MENU).filter { it.canteenId == canteen?.id },
            key = { "menu${it.id}" },
        ) { item ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${money(item.pricePaise)} | ${if(item.available) "Available" else "Unavailable"}"
                    )
                    if (vendor)
                        Button(
                            onClick = {
                                vm.action(
                                    "setMenuAvailability",
                                    mapOf("menuItemId" to item.id, "available" to !item.available),
                                )
                            },
                            enabled = !state.busy,
                        ) {
                            Text(if (item.available) "Mark unavailable" else "Make available")
                        }
                    else
                        Button(
                            onClick = {
                                vm.add(item, 1)
                                requestId = java.util.UUID.randomUUID().toString()
                            },
                            enabled = item.available && canteen?.open == true && !state.busy,
                        ) {
                            Text("Add to cart")
                        }
                }
            }
        }
        item {
            Text(
                if (vendor) "Order queue" else "Order history",
                style = MaterialTheme.typography.titleLarge,
            )
        }
        feedState(CampusFeed.ORDERS, state, vm, "No orders yet.")
        items(
            orders.filter {
                (orderId == null || it.id == orderId) &&
                    (statusFilter == null || it.status == statusFilter)
            },
            key = { it.id },
        ) { order ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Token ${order.token}", style = MaterialTheme.typography.titleLarge)
                    Text(
                        order.status.name.replace('_', ' '),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(dateTime(order.createdAt))
                    order.items.forEach { Text("${it.name} | ${it.quantity}") }
                    Text("${money(order.totalPaise)} | Pay at counter")
                    if (vendor)
                        FoodOrderStatus.entries
                            .filter { order.status.canTransitionTo(it) }
                            .forEach { next ->
                                Button(
                                    onClick = {
                                        vm.action(
                                            "updateFoodOrder",
                                            mapOf("orderId" to order.id, "status" to next.name),
                                        )
                                    },
                                    enabled = !state.busy,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                                ) {
                                    Text(next.name.lowercase().replaceFirstChar { it.uppercase() })
                                }
                            }
                    else if (order.status == FoodOrderStatus.PLACED)
                        OutlinedButton(
                            onClick = {
                                vm.action(
                                    "updateFoodOrder",
                                    mapOf("orderId" to order.id, "status" to "CANCELLED"),
                                )
                            },
                            enabled = !state.busy,
                        ) {
                            Text("Cancel order")
                        }
                }
            }
        }
    }
}
