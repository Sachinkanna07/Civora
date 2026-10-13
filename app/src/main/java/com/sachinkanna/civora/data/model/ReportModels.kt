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
    ACCEPTED,
    IN_PROGRESS,
    REVIEW_REQUIRED,
    RESOLVED,
    REJECTED;

    fun canTransitionTo(next: ReportStatus): Boolean =
        when (this) {
            SUBMITTED -> next == ASSIGNED || next == REJECTED
            ASSIGNED -> next == ACCEPTED || next == IN_PROGRESS || next == REJECTED
            ACCEPTED -> next == IN_PROGRESS || next == REJECTED
            IN_PROGRESS -> next == REVIEW_REQUIRED || next == RESOLVED || next == REJECTED
            REVIEW_REQUIRED -> next == RESOLVED || next == IN_PROGRESS || next == REJECTED
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
    val imagePath: String = "",
    val completionImagePath: String = "",
    val assignedToName: String = "",
    val progressNote: String = "",
    val priority: String = "normal",
    val updatedAt: Long = 0,
)
