package com.communityos.authentication.model

data class AuthUser(
    val uid: String,
    val phoneNumber: String,
    val displayName: String? = null,
    val email: String? = null,
    val isNewUser: Boolean = false
)
