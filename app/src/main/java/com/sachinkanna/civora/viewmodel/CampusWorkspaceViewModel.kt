package com.sachinkanna.civora.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sachinkanna.civora.data.model.*
import com.sachinkanna.civora.data.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class FeedState(
    val loading: Boolean = true,
    val items: List<Any> = emptyList(),
    val error: String? = null,
) {
    inline fun <reified T> values(): List<T> = items.filterIsInstance<T>()
}

data class WorkspaceState(
    val feeds: Map<CampusFeed, FeedState> = emptyMap(),
    val busy: Boolean = false,
    val message: String? = null,
    val actionError: Boolean = false,
    val cart: List<CartItem> = emptyList(),
    val invitationLink: String? = null,
    val cartRequestId: String = java.util.UUID.randomUUID().toString(),
    val lastAction: String = "",
    val lastResult: Map<String, Any?> = emptyMap(),
    val lastOrderId: String? = null,
) {
    fun feed(key: CampusFeed) = feeds[key] ?: FeedState()

    inline fun <reified T> values(key: CampusFeed) = feed(key).values<T>()
}

class CampusWorkspaceViewModel(
    private val gateway: CampusGateway = LiveCampusRepository(),
    private val carts: CartPersistence = MemoryCartPersistence(),
    private val logFailure: (String, Exception) -> Unit = { message, error ->
        Log.w("Civora", message, error)
        Unit
    },
) : ViewModel() {
    private val mutable = MutableStateFlow(WorkspaceState())
    val state = mutable.asStateFlow()
    private val jobs = mutableMapOf<CampusFeed, Job>()
    private var actionJob: Job? = null
    private var sessionVersion = 0L
    private var profile: UserProfile? = null

    fun connect(p: UserProfile) {
        if (profile == p) return
        sessionVersion++
        actionJob?.cancel()
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        profile = p
        val restored = if(p.role == UserRole.STUDENT) carts.load(p.uid) else PersistedCart()
        mutable.value = WorkspaceState(cart = restored.items, cartRequestId = restored.requestId)
        val feeds =
            when (p.role) {
                UserRole.STUDENT -> CampusFeed.entries.filter { it !in listOf(CampusFeed.STAFF,CampusFeed.STUDENTS) }
                UserRole.VENDOR -> listOf(CampusFeed.CANTEENS, CampusFeed.MENU, CampusFeed.ORDERS, CampusFeed.INBOX)
                UserRole.DRIVER -> listOf(CampusFeed.BUSES, CampusFeed.ROUTES, CampusFeed.TRIPS, CampusFeed.TRANSPORT_UPDATES)
                UserRole.MAINTENANCE -> listOf(CampusFeed.REPORTS, CampusFeed.LOCATIONS, CampusFeed.INBOX)
                UserRole.MESS -> listOf(CampusFeed.MESS_HALLS, CampusFeed.MEAL_PERIODS, CampusFeed.MEAL_CHECKINS, CampusFeed.INBOX)
                UserRole.FACULTY ->
                    listOf(
                        CampusFeed.EVENTS,
                        CampusFeed.REGISTRATIONS,
                        CampusFeed.ANNOUNCEMENTS,
                        CampusFeed.TIMETABLE, CampusFeed.CLUBS, CampusFeed.LOCATIONS, CampusFeed.DEPARTMENTS,
                    )
                UserRole.ADMIN ->
                    listOf(
                        CampusFeed.REGISTRATIONS,
                        CampusFeed.TIMETABLE,
                        CampusFeed.EVENTS,
                        CampusFeed.ANNOUNCEMENTS,
                        CampusFeed.REPORTS,
                        CampusFeed.BUSES,
                        CampusFeed.ROUTES,
                        CampusFeed.TRIPS,
                        CampusFeed.CANTEENS,
                        CampusFeed.CLUBS,
                        CampusFeed.LOCATIONS,
                        CampusFeed.MENU, CampusFeed.STAFF, CampusFeed.STUDENTS, CampusFeed.DEPARTMENTS, CampusFeed.CALENDAR, CampusFeed.TRANSPORT_UPDATES, CampusFeed.MESS_HALLS, CampusFeed.MEAL_PERIODS, CampusFeed.ENTITLEMENTS,
                    )
            }
        feeds.forEach(::retry)
    }

    fun retry(feed: CampusFeed) {
        val p = profile ?: return
        jobs.remove(feed)?.cancel()
        mutable.update {
            it.copy(feeds = it.feeds + (feed to it.feed(feed).copy(loading = true, error = null)))
        }
        jobs[feed] = viewModelScope.launch {
            try {
                gateway.observe(feed, p).collect { values ->
                    mutable.update {
                        it.copy(feeds = it.feeds + (feed to FeedState(false, values)),
                            cart = if(feed == CampusFeed.MENU) it.cart.map { cart -> cart.copy(item = values.filterIsInstance<MenuItem>().find { m -> m.id == cart.item.id } ?: cart.item.copy(available = false)) } else it.cart)
                    }
                    if(feed == CampusFeed.MENU) persistCart()
                    if(feed == CampusFeed.ORDERS && values.filterIsInstance<FoodOrder>().any { it.id == "${p.uid}_${mutable.value.cartRequestId}" }) clearCart()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logFailure("Feed $feed unavailable", e)
                mutable.update {
                    it.copy(
                        feeds =
                            it.feeds +
                                (feed to
                                    it.feed(feed)
                                        .copy(
                                            loading = false,
                                            error =
                                                "Could not load this information. Check your connection and retry.",
                                        ))
                    )
                }
            }
        }
    }

    fun disconnect() {
        profile?.uid?.let(carts::clear)
        sessionVersion++
        actionJob?.cancel()
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        profile = null
        mutable.value = WorkspaceState()
    }

    fun dismiss() {
        mutable.update { it.copy(message = null) }
    }

    fun action(
        name: String,
        data: Map<String, Any?>,
        success: String = "Updated",
        after: () -> Unit = {},
    ) = runAction {
        val result = gateway.action(name, data)
        mutable.update {
            it.copy(
                message = success,
                lastAction = name,
                lastResult = result,
                lastOrderId = if(name == "placeFoodOrder") result["orderId"] as? String else it.lastOrderId,
                actionError = false,
                invitationLink = result["setupLink"] as? String ?: it.invitationLink,
            )
        }
        after()
    }

    private fun runAction(block: suspend () -> Unit) {
        if (mutable.value.busy) return
        val version = sessionVersion
        mutable.update { it.copy(busy = true, message = null) }
        actionJob = viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logFailure("Action failed", e)
                mutable.update {
                    it.copy(
                        message =
                            if(e is com.google.firebase.functions.FirebaseFunctionsException && e.code in listOf(com.google.firebase.functions.FirebaseFunctionsException.Code.FAILED_PRECONDITION, com.google.firebase.functions.FirebaseFunctionsException.Code.INVALID_ARGUMENT, com.google.firebase.functions.FirebaseFunctionsException.Code.ALREADY_EXISTS)) e.message ?: "Check the information and try again."
                            else if(e is com.google.firebase.functions.FirebaseFunctionsException && e.code == com.google.firebase.functions.FirebaseFunctionsException.Code.PERMISSION_DENIED) "This action is not available for your account."
                            else "Confirmation was interrupted. Check the latest status, then retry if needed.",
                        actionError = true,
                    )
                }
            } finally {
                if (version == sessionVersion) mutable.update { it.copy(busy = false) }
            }
        }
    }

    fun add(item: MenuItem, delta: Int) {
        if(mutable.value.busy) return
        if (!item.available && delta > 0) return
        mutable.update { s ->
            if (s.cart.isNotEmpty() && s.cart.first().item.canteenId != item.canteenId)
                s.copy(
                    message = "Clear your cart before ordering from another canteen.",
                    actionError = true,
                )
            else {
                val quantity =
                    ((s.cart.find { it.item.id == item.id }?.quantity ?: 0) + delta).coerceIn(0, 20)
                s.copy(
                    cartRequestId = java.util.UUID.randomUUID().toString(),
                    cart =
                        s.cart.filterNot { it.item.id == item.id } +
                            if (quantity > 0) listOf(CartItem(item, quantity)) else emptyList()
                )
            }
        }
        persistCart()
    }

    private fun persistCart() { profile?.uid?.let { carts.save(it, PersistedCart(mutable.value.cartRequestId,mutable.value.cart)) } }
    fun clearCart() {
        mutable.update { it.copy(cart = emptyList(), cartRequestId = java.util.UUID.randomUUID().toString()) }
        persistCart()
    }

    fun placeOrder(requestId: String = mutable.value.cartRequestId) {
        val cart = mutable.value.cart
        if (cart.isEmpty()) return
        action(
            "placeFoodOrder",
            mapOf(
                "requestId" to requestId,
                "canteenId" to cart.first().item.canteenId,
                "expectedTotalPaise" to cartTotal(cart),
                "items" to cart.map { mapOf("menuItemId" to it.item.id, "quantity" to it.quantity) },
            ),
            "Order placed. Pay at the counter.",
            ::clearCart,
        )
    }

    fun follow(clubId: String) = runAction {
        val uid = profile?.uid ?: return@runAction
        gateway.follow(uid, clubId, clubId in mutable.value.values<String>(CampusFeed.FOLLOWS))
    }

    companion object {
        fun factory(context: android.content.Context) = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = CampusWorkspaceViewModel(carts = DeviceCartPersistence(context.applicationContext)) as T
        }
    }

    fun read(id: String) = runAction { gateway.markRead(profile?.uid.orEmpty(), id) }

    fun saveProfile(values: Map<String, Any?>, after: () -> Unit) = runAction {
        gateway.updateProfile(profile?.uid.orEmpty(), values)
        mutable.update { it.copy(message = "Profile saved.", actionError = false) }
        after()
    }
}
