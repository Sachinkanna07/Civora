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
    val effectiveFrom: String = "2000-01-01",
    val effectiveTo: String = "2099-12-31",
)

data class AcademicOverride(val id:String="",val date:String="",val kind:String="CLASS",val baseCourseId:String="",val courseId:String="",val courseName:String="",val faculty:String="",val startMinute:Int=0,val endMinute:Int=0,val room:String="",val department:String="",val year:String="",val section:String="",val reason:String="")

fun resolvedClassesForDate(entries:List<TimetableEntry>,overrides:List<AcademicOverride>,date:LocalDate):List<TimetableEntry>{
    val active=entries.filter {it.dayOfWeek==date.dayOfWeek.value&&runCatching {date>=LocalDate.parse(it.effectiveFrom)&&date<=LocalDate.parse(it.effectiveTo)}.getOrDefault(false)}
    val day=overrides.filter {it.date==date.toString()}
    val replaced=day.map {it.baseCourseId}.filter {it.isNotBlank()}.toSet()
    return (active.filter {it.courseId !in replaced}+day.filter {it.kind=="CLASS"}.map {TimetableEntry("override_${it.id}",it.courseId,it.courseName,it.faculty,date.dayOfWeek.value,it.startMinute,it.endMinute,it.room,it.department,it.year,it.section,date.toString(),date.toString())}).sortedBy {it.startMinute}
}

fun nextAcademicClass(entries:List<TimetableEntry>,overrides:List<AcademicOverride>,now:LocalDateTime,holidays:Set<LocalDate>):ScheduledClass?=(0..14).flatMap {offset->val date=now.toLocalDate().plusDays(offset.toLong());if(date in holidays)emptyList() else resolvedClassesForDate(entries,overrides,date).map {ScheduledClass(it,date.atStartOfDay().plusMinutes(it.startMinute.toLong()))}}.filter {it.date>=now}.minByOrNull {it.date}

data class ScheduledClass(val entry: TimetableEntry, val date: LocalDateTime)

fun nextClass(entries: List<TimetableEntry>, now: LocalDateTime, holidays: Set<LocalDate> = emptySet()): ScheduledClass? =
    (0..7)
        .flatMap { offset ->
            val date = now.toLocalDate().plusDays(offset.toLong())
            entries
                .filter {
                    date !in holidays && it.dayOfWeek == date.dayOfWeek.value &&
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
    TIMETABLE,
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
