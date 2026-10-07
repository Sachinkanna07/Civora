package com.sachinkanna.civora.data.model

data class Announcement(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = "General",
    val priority: String = "normal",
    val audience: String = "students",
    val department: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val createdAt: Long = 0L,
    val expiresAt: Long? = null,
)

data class CampusEvent(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "Campus",
    val department: String = "",
    val venue: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val imageUrl: String = "",
    val organizer: String = "",
    val capacity: Int = 0,
    val registrationEnabled: Boolean = true,
    val createdBy: String = "",
    val clubId: String = "",
    val registeredCount: Int = 0,
)
