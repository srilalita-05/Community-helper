package com.communityos.complaints.model

enum class ComplaintStatus {
    SUBMITTED,
    IN_PROGRESS,
    RESOLVED;

    companion object {
        fun fromString(value: String?): ComplaintStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SUBMITTED
        }
    }
}
