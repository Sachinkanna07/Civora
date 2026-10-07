package com.sachinkanna.civora.data.model

import java.time.*

data class TimetableEntry(
    val id: String = "",
    val courseId: String = "",
    val courseName: String = "",
    val faculty: String = "",
    val dayOfWeek: Int = 1,
    val startMinute: Int = 540,
    val endMinute: Int = 590,
    val room: String = "",
    val department: String = "",
    val year: String = "",
    val section: String = "",
)

data class ScheduledClass(val entry: TimetableEntry, val date: LocalDateTime)

fun nextClass(entries: List<TimetableEntry>, now: LocalDateTime): ScheduledClass? =
    (0..7)
        .flatMap { offset ->
            val date = now.toLocalDate().plusDays(offset.toLong())
            entries
                .filter {
                    it.dayOfWeek == date.dayOfWeek.value &&
                        it.startMinute in 0..1439 &&
                        it.endMinute in 1..1440 &&
                        it.endMinute > it.startMinute
                }
                .map {
                    ScheduledClass(it, date.atStartOfDay().plusMinutes(it.startMinute.toLong()))
                }
        }
        .filter { it.date >= now }
        .minByOrNull { it.date }

fun clockTime(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)

/** Merge overlapping classes before finding gaps within a single day's schedule. */
fun freePeriods(entries: List<TimetableEntry>): List<IntRange> {
    val sorted = entries.filter { it.endMinute > it.startMinute }.sortedBy { it.startMinute }
    if (sorted.isEmpty()) return emptyList()
    val gaps = mutableListOf<IntRange>()
    var occupiedUntil = sorted.first().endMinute
    sorted.drop(1).forEach { entry ->
        if (entry.startMinute > occupiedUntil) gaps += occupiedUntil until entry.startMinute
        occupiedUntil = maxOf(occupiedUntil, entry.endMinute)
    }
    return gaps
}

enum class ActivityType {
    EVENT_REGISTERED,
    EVENT_CANCELLED,
    EVENT_ATTENDED,
    FOOD_ORDER,
    REPORT,
    NOTICE,
}

enum class NotificationCategory {
    URGENT,
    RELEVANT,
    DISCOVER,
}

enum class Destination {
    EVENT,
    FOOD,
    REPORT,
    ANNOUNCEMENT,
    TRANSPORT,
}

data class CampusActivity(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: ActivityType = ActivityType.NOTICE,
    val status: String = "",
    val createdAt: Long = 0,
    val destination: Destination? = null,
    val entityId: String = "",
)

data class InboxItem(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val category: NotificationCategory = NotificationCategory.RELEVANT,
    val createdAt: Long = 0,
    val read: Boolean = false,
    val destination: Destination? = null,
    val entityId: String = "",
)

data class Club(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val imageUrl: String = "",
)

data class CampusLocation(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val description: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
)
