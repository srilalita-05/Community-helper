package com.communityos.maintenance.model

enum class BillStatus {
    UNPAID,
    PAID,
    OVERDUE;

    companion object {
        fun fromString(value: String): BillStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNPAID
        }
    }
}
