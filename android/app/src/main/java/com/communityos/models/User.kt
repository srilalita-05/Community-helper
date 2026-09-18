package com.communityos.models

/**
 * Canonical Domain representation of a User in the Community OS ecosystem.
 *
 * Why this is the canonical model:
 * 1. Unifies user identity across all functional modules (Auth, Resident, Security, Admin).
 * 2. Strongly types user roles via [UserRole] instead of loose strings.
 * 3. Bridges personal identity with community and flat mapping relationships.
 * 4. Tracks society administrative verification status ([isApproved]).
 */
data class User(
    val id: String,
    val phoneNumber: String,
    val name: String,
    val email: String? = null,
    val role: UserRole = UserRole.RESIDENT,
    val communityId: String? = null,
    val flatId: String? = null,
    val isApproved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
