package com.sachinkanna.civora.data.model

/** Firestore user profile entity. */
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.STUDENT,
    val department: String = "",
    val year: String = "",
    val profileImage: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val section: String = "",
    val routeId: String = "",
    val notificationsEnabled: Boolean = true,
)
