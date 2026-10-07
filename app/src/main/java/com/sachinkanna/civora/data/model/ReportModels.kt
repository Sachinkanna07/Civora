package com.sachinkanna.civora.data.model

enum class ReportCategory {
    ELECTRICAL,
    WATER,
    CLEANLINESS,
    SAFETY,
    INFRASTRUCTURE,
    TRANSPORT,
    OTHER,
}

enum class ReportStatus {
    SUBMITTED,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    REJECTED;

    fun canTransitionTo(next: ReportStatus): Boolean =
        when (this) {
            SUBMITTED -> next == ASSIGNED || next == REJECTED
            ASSIGNED -> next == IN_PROGRESS || next == REJECTED
            IN_PROGRESS -> next == RESOLVED || next == REJECTED
            else -> false
        }
}

data class CampusReport(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: ReportCategory = ReportCategory.OTHER,
    val imageUrl: String = "",
    val location: String = "",
    val createdBy: String = "",
    val createdAt: Long = 0,
    val status: ReportStatus = ReportStatus.SUBMITTED,
    val assignedTo: String = "",
    val resolutionNote: String = "",
)
