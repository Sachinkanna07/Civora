package com.sachinkanna.civora.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import com.google.firebase.functions.FirebaseFunctions
import com.sachinkanna.civora.data.model.*
import kotlinx.coroutines.tasks.await

fun DocumentSnapshot.time(field: String): Long =
    getTimestamp(field)?.toDate()?.time ?: (get(field) as? Number)?.toLong() ?: 0

fun DocumentSnapshot.announcement() =
    Announcement(
        id,
        getString("title").orEmpty(),
        getString("body").orEmpty(),
        getString("category") ?: "General",
        getString("priority") ?: "normal",
        getString("audience") ?: "students",
        getString("department").orEmpty(),
        getString("authorId").orEmpty(),
        getString("authorName").orEmpty(),
        time("createdAt"),
        time("expiresAt").takeIf { it > 0 },
    )

fun DocumentSnapshot.event() =
    CampusEvent(
        id,
        getString("title").orEmpty(),
        getString("description").orEmpty(),
        getString("category") ?: "Campus",
        getString("department").orEmpty(),
        getString("venue").orEmpty(),
        time("startTime"),
        time("endTime"),
        getString("imageUrl").orEmpty(),
        getString("organizer").orEmpty(),
        (getLong("capacity") ?: 0).toInt(),
        getBoolean("registrationEnabled") ?: false,
        getString("createdBy").orEmpty(),
        getString("clubId").orEmpty(),
        (getLong("registeredCount") ?: 0).toInt(),
    )

class AnnouncementRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    suspend fun getFeed(department: String = ""): List<Announcement> =
        db.collection("announcements")
            .whereEqualTo("audience", "students")
            .whereIn("department", listOf("", department).distinct())
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .await()
            .documents
            .map { it.announcement() }

    suspend fun get(id: String): Announcement {
        val doc = db.collection("announcements").document(id).get().await()
        check(doc.exists())
        return doc.announcement()
    }
}

class EventsRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    suspend fun getUpcoming(): List<CampusEvent> =
        db.collection("events")
            .whereGreaterThanOrEqualTo("endTime", Timestamp.now())
            .orderBy("endTime")
            .limit(100)
            .get()
            .await()
            .documents
            .map { it.event() }

    suspend fun registeredEventIds(uid: String): Set<String> =
        db.collection("eventRegistrations")
            .whereEqualTo("userId", uid)
            .limit(200)
            .get()
            .await()
            .documents
            .mapNotNull { it.getString("eventId") }
            .toSet()

    suspend fun get(id: String): CampusEvent {
        val doc = db.collection("events").document(id).get().await()
        check(doc.exists())
        return doc.event()
    }

    suspend fun register(eventId: String, uid: String) {
        FirebaseFunctions.getInstance()
            .getHttpsCallable("eventRegistration")
            .call(mapOf("eventId" to eventId, "cancel" to false))
            .await()
    }

    suspend fun cancel(eventId: String, uid: String) {
        FirebaseFunctions.getInstance()
            .getHttpsCallable("eventRegistration")
            .call(mapOf("eventId" to eventId, "cancel" to true))
            .await()
    }
}
