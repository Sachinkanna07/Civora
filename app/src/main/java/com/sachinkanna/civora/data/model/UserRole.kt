package com.sachinkanna.civora.data.model

/** Supported user roles in the Civora Campus Super-App. */
enum class UserRole(
    val roleKey: String,
    val displayName: String,
    val description: String,
) {
    STUDENT("student", "Student", "Access courses, bus tracking, food ordering & campus events"),
    FACULTY("faculty", "Faculty", "Manage announcements, attendance & departmental updates"),
    ADMIN("admin", "Admin", "Supervise campus operations, user roles & emergency alerts"),
    VENDOR("vendor", "Vendor", "Manage canteen items, inventory & incoming student orders"),
    DRIVER("driver", "Driver", "Vehicle, trip and transport updates"),
    MAINTENANCE("maintenance", "Maintenance", "Assigned campus repairs and work history"),
    MESS("mess", "Mess operator", "Meal eligibility and check-in operations");

    companion object {
        fun fromKey(key: String?): UserRole? = entries.find {
            it.roleKey.equals(key, ignoreCase = true)
        }
    }
}
