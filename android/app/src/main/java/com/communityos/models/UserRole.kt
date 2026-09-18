package com.communityos.models

/**
 * Strongly-typed role representation for role-based access control (RBAC).
 */
enum class UserRole {
    RESIDENT,
    ADMIN,
    SECURITY;

    companion object {
        fun fromString(role: String?): UserRole {
            return when (role?.trim()?.uppercase()) {
                "ADMIN" -> ADMIN
                "SECURITY" -> SECURITY
                else -> RESIDENT
            }
        }
    }
}
