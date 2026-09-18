package com.communityos.data.local.session

import com.communityos.models.UserRole

/**
 * Lightweight representation of the currently authenticated session.
 * Room remains the source of truth for full user details;
 * DataStore tracks only active authentication context.
 */
data class Session(
    val userId: String,
    val role: UserRole
)
