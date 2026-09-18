package com.communityos.authentication.model

import com.communityos.models.User
import com.communityos.models.UserRole

/**
 * Authentication/UI representation of a user.
 * Kept for backward compatibility with existing Auth screens and ViewModels.
 * Bridges with canonical [User] domain model.
 */
data class AuthUser(
    val uid: String,
    val phoneNumber: String,
    val displayName: String? = null,
    val email: String? = null,
    val isNewUser: Boolean = false,
    val role: UserRole = UserRole.RESIDENT
)

fun User.toAuthUser(isNewUser: Boolean = false): AuthUser = AuthUser(
    uid = id,
    phoneNumber = phoneNumber,
    displayName = name,
    email = email,
    isNewUser = isNewUser,
    role = role
)

fun AuthUser.toDomain(
    role: UserRole = this.role,
    communityId: String? = null,
    flatId: String? = null,
    isApproved: Boolean = false
): User = User(
    id = uid,
    phoneNumber = phoneNumber,
    name = displayName ?: "",
    email = email,
    role = role,
    communityId = communityId,
    flatId = flatId,
    isApproved = isApproved
)
