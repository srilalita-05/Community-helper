package com.communityos.visitors.model

enum class VisitorStatus {
    PRE_APPROVED,
    CHECKED_IN,
    CHECKED_OUT,
    DENIED,
    CANCELLED;

    companion object {
        fun fromString(value: String?): VisitorStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PRE_APPROVED
        }
    }
}
