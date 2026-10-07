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
) {
    fun feed(key: CampusFeed) = feeds[key] ?: FeedState()

    inline fun <reified T> values(key: CampusFeed) = feed(key).values<T>()
}

class CampusWorkspaceViewModel(
    private val gateway: CampusGateway = LiveCampusRepository(),
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
        mutable.value = WorkspaceState()
        val feeds =
            when (p.role) {
                UserRole.STUDENT -> CampusFeed.entries.toList()
                UserRole.VENDOR -> listOf(CampusFeed.CANTEENS, CampusFeed.MENU, CampusFeed.ORDERS)
                UserRole.DRIVER -> listOf(CampusFeed.BUSES, CampusFeed.ROUTES, CampusFeed.TRIPS)
                UserRole.FACULTY ->
                    listOf(
                        CampusFeed.EVENTS,
                        CampusFeed.REGISTRATIONS,
                        CampusFeed.ANNOUNCEMENTS,
                        CampusFeed.TIMETABLE,
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
                        CampusFeed.MENU,
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
                        it.copy(feeds = it.feeds + (feed to FeedState(false, values)))
                    }
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
                            "Could not complete the action. Refresh the information and try again.",
                        actionError = true,
                    )
                }
            } finally {
                if (version == sessionVersion) mutable.update { it.copy(busy = false) }
            }
        }
    }

    fun add(item: MenuItem, delta: Int) {
        if (!item.available) return
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
                    cart =
                        s.cart.filterNot { it.item.id == item.id } +
                            if (quantity > 0) listOf(CartItem(item, quantity)) else emptyList()
                )
            }
        }
    }

    fun clearCart() {
        mutable.update { it.copy(cart = emptyList()) }
    }

    fun placeOrder(requestId: String) {
        val cart = mutable.value.cart
        if (cart.isEmpty()) return
        action(
            "placeFoodOrder",
            mapOf(
                "requestId" to requestId,
                "canteenId" to cart.first().item.canteenId,
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

    fun read(id: String) = runAction { gateway.markRead(profile?.uid.orEmpty(), id) }

    fun saveProfile(values: Map<String, Any?>, after: () -> Unit) = runAction {
        gateway.updateProfile(profile?.uid.orEmpty(), values)
        mutable.update { it.copy(message = "Profile saved.", actionError = false) }
        after()
    }
}
