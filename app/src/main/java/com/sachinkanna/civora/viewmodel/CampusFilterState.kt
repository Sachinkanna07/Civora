package com.sachinkanna.civora.viewmodel

import com.sachinkanna.civora.data.model.*

data class AnnouncementUiState(
    val loading: Boolean = true,
    val items: List<Announcement> = emptyList(),
    val category: String? = null,
    val departmentOnly: Boolean = false,
    val error: String? = null,
    val department: String = "",
) {
    val filtered
        get() = items.filter {
            (category == null || it.category.equals(category, true)) &&
                (!departmentOnly || (department.isNotBlank() && it.department == department)) &&
                (it.expiresAt == null || it.expiresAt > System.currentTimeMillis())
        }
}

data class EventsUiState(
    val loading: Boolean = true,
    val items: List<CampusEvent> = emptyList(),
    val selectedCategory: String? = null,
    val upcomingOnly: Boolean = true,
    val registeredIds: Set<String> = emptySet(),
    val error: String? = null,
    val actionInProgress: String? = null,
) {
    val filtered
        get() = items.filter {
            (selectedCategory == null || it.category.equals(selectedCategory, true)) &&
                (!upcomingOnly || it.endTime > System.currentTimeMillis())
        }
}
