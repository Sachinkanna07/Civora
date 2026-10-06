package com.sachinkanna.civora.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.sachinkanna.civora.data.model.Announcement
import com.sachinkanna.civora.data.model.CampusEvent
import kotlinx.coroutines.tasks.await

class AnnouncementRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    suspend fun getFeed(): List<Announcement> = db.collection("announcements").get().await().documents.map { d -> Announcement(d.id, d.getString("title") ?: "", d.getString("body") ?: "", d.getString("category") ?: "General", d.getString("priority") ?: "normal", d.getString("audience") ?: "students", d.getString("department") ?: "", d.getString("authorId") ?: "", d.getString("authorName") ?: "", d.getTimestamp("createdAt")?.toDate()?.time ?: 0L, d.getTimestamp("expiresAt")?.toDate()?.time) }
    suspend fun get(id:String): Announcement = getFeed().first { it.id == id }
}

class EventsRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    suspend fun getUpcoming(): List<CampusEvent> = db.collection("events").get().await().documents.map { d -> CampusEvent(d.id, d.getString("title") ?: "", d.getString("description") ?: "", d.getString("category") ?: "Campus", d.getString("department") ?: "", d.getString("venue") ?: "", d.getTimestamp("startTime")?.toDate()?.time ?: 0L, d.getTimestamp("endTime")?.toDate()?.time ?: 0L, d.getString("imageUrl") ?: "", d.getString("organizer") ?: "", (d.getLong("capacity") ?: 0L).toInt(), d.getBoolean("registrationEnabled") ?: true, d.getString("createdBy") ?: "") }
    suspend fun registeredEventIds(uid: String): Set<String> = db.collection("eventRegistrations").whereEqualTo("userId", uid).get().await().documents.mapNotNull { it.getString("eventId") }.toSet()
    suspend fun get(id:String): CampusEvent = getUpcoming().first { it.id == id }
    suspend fun register(eventId: String, uid: String) {
        val ref = db.collection("eventRegistrations").document("${eventId}_$uid")
        if (!ref.get().await().exists()) ref.set(mapOf("eventId" to eventId, "userId" to uid)).await()
    }
    suspend fun cancel(eventId: String, uid: String) { db.collection("eventRegistrations").document("${eventId}_$uid").delete().await() }
}
