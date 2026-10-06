package com.sachinkanna.civora.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.sachinkanna.civora.data.model.Announcement
import com.sachinkanna.civora.data.model.CampusEvent
import kotlinx.coroutines.tasks.await

class AnnouncementRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    suspend fun getFeed(): List<Announcement> = db.collection("announcements").get().await().documents.map { d -> Announcement(d.id, d.getString("title") ?: "", d.getString("body") ?: "", d.getString("category") ?: "General", d.getString("priority") ?: "normal", d.getString("audience") ?: "students", d.getString("department") ?: "", d.getString("authorId") ?: "", d.getString("authorName") ?: "", d.getTimestamp("createdAt")?.toDate()?.time ?: 0L, d.getTimestamp("expiresAt")?.toDate()?.time) }
}

class EventsRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    suspend fun getUpcoming(): List<CampusEvent> = db.collection("events").get().await().documents.map { d -> CampusEvent(d.id, d.getString("title") ?: "", d.getString("description") ?: "", d.getString("category") ?: "Campus", d.getString("department") ?: "", d.getString("venue") ?: "", d.getTimestamp("startTime")?.toDate()?.time ?: 0L, d.getTimestamp("endTime")?.toDate()?.time ?: 0L, d.getString("imageUrl") ?: "", d.getString("organizer") ?: "", (d.getLong("capacity") ?: 0L).toInt(), d.getBoolean("registrationEnabled") ?: true, d.getString("createdBy") ?: "") }
    suspend fun register(eventId: String, uid: String) { db.collection("eventRegistrations").document("${eventId}_$uid").set(mapOf("eventId" to eventId, "userId" to uid)).await() }
}
