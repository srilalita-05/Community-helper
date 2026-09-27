package com.communityos.marketplace.model

enum class ListingStatus(val displayName: String) {
    ACTIVE("Active"),
    SOLD("Sold"),
    CANCELLED("Cancelled");

    companion object {
        fun fromString(value: String): ListingStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: ACTIVE
        }
    }
}
