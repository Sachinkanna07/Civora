package com.sachinkanna.civora.data.model

import java.time.*

data class StaffMember(val uid: String = "", val name: String = "", val role: String = "", val department: String = "", val team: String = "", val active: Boolean = true)
data class Department(val id: String = "", val name: String = "", val code: String = "", val years: List<String> = listOf("1", "2", "3", "4"), val sections: List<String> = listOf("A", "B"))
data class CalendarDay(val id: String = "", val date: String = "", val title: String = "", val department: String = "", val holiday: Boolean = true)
data class TransportUpdate(val id: String = "", val busId: String = "", val routeId: String = "", val busName: String = "", val kind: String = "", val delayMinutes: Int = 0, val note: String = "", val createdAt: Long = 0)
data class MessHall(val id: String = "", val name: String = "", val operatorId: String = "", val open: Boolean = false)
data class MealPeriod(val id: String = "", val messId: String = "", val label: String = "", val opensAt: Long = 0, val closesAt: Long = 0)
data class MealEntitlement(val id: String = "", val messId: String = "", val userId: String = "", val active: Boolean = false)
data class MealCheckIn(val id: String = "", val periodId: String = "", val messId: String = "", val userId: String = "", val label: String = "", val createdAt: Long = 0)
data class WorkHistory(val id: String, val status: String, val note: String, val createdAt: Long, val evidencePath: String = "")

fun selectedBoardingStop(route: BusRoute?, stopName: String): BusStop? = route?.stops?.find { it.name == stopName }
fun currentClass(entries: List<TimetableEntry>, now: LocalDateTime): TimetableEntry? {
    val minute = now.hour * 60 + now.minute
    return entries.filter { it.dayOfWeek == now.dayOfWeek.value && minute >= it.startMinute && minute < it.endMinute }.minByOrNull { it.endMinute }
}
fun holidayDates(days: List<CalendarDay>, department: String): Set<LocalDate> = days.filter { it.holiday && (it.department.isBlank() || it.department == department) }.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSet()
fun kitchenQueue(orders: List<FoodOrder>, queue: String): List<FoodOrder> = orders.filter {
    when (queue) { "New" -> it.status in listOf(FoodOrderStatus.PLACED, FoodOrderStatus.ACCEPTED); "Preparing" -> it.status == FoodOrderStatus.PREPARING; "Ready" -> it.status == FoodOrderStatus.READY; else -> !it.status.active }
}.sortedBy { it.createdAt }
fun nextKitchenStatus(status: FoodOrderStatus): FoodOrderStatus? = when(status) {
    FoodOrderStatus.PLACED -> FoodOrderStatus.ACCEPTED
    FoodOrderStatus.ACCEPTED -> FoodOrderStatus.PREPARING
    FoodOrderStatus.PREPARING -> FoodOrderStatus.READY
    FoodOrderStatus.READY -> FoodOrderStatus.COMPLETED
    else -> null
}
fun parseTicketPayload(value: String, prefix: String = "civora"): Pair<String, String>? {
    val parts = value.trim().split(':')
    if(parts.size != 3 || parts[0] != prefix || !parts[1].matches(Regex("[A-Za-z0-9_-]{1,257}")) || !parts[2].matches(Regex("[a-fA-F0-9]{48}"))) return null
    return parts[1] to parts[2]
}
