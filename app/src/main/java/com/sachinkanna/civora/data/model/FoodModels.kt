package com.sachinkanna.civora.data.model

data class Canteen(
    val id: String = "",
    val name: String = "",
    val vendorId: String = "",
    val open: Boolean = false,
)

data class MenuItem(
    val id: String = "",
    val canteenId: String = "",
    val name: String = "",
    val pricePaise: Long = 0,
    val available: Boolean = false,
    val imageUrl: String = "",
)

data class CartItem(val item: MenuItem, val quantity: Int)

data class FoodOrderItem(
    val menuItemId: String = "",
    val name: String = "",
    val quantity: Int = 0,
    val pricePaise: Long = 0,
)

enum class FoodOrderStatus {
    PLACED,
    ACCEPTED,
    PREPARING,
    READY,
    COMPLETED,
    CANCELLED;

    fun canTransitionTo(next: FoodOrderStatus): Boolean =
        when (this) {
            PLACED -> next == ACCEPTED || next == CANCELLED
            ACCEPTED -> next == PREPARING || next == CANCELLED
            PREPARING -> next == READY
            READY -> next == COMPLETED
            else -> false
        }

    val active
        get() = this != COMPLETED && this != CANCELLED
}

data class FoodOrder(
    val id: String = "",
    val userId: String = "",
    val canteenId: String = "",
    val token: String = "",
    val status: FoodOrderStatus = FoodOrderStatus.PLACED,
    val items: List<FoodOrderItem> = emptyList(),
    val totalPaise: Long = 0,
    val createdAt: Long = 0,
    val paymentState: String = "PAY_AT_COUNTER",
)

fun cartTotal(items: List<CartItem>): Long = items.sumOf { it.item.pricePaise * it.quantity }

fun money(paise: Long): String = "INR %.2f".format(paise / 100.0)
