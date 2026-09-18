package com.communityos.profile.model

import com.communityos.models.UserRole

data class UserProfile(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val email: String?,
    val communityName: String,
    val flatNumber: String,
    val role: UserRole
)
