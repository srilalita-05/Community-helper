package com.communityos.authentication.state

import com.communityos.authentication.model.AuthUser

data class AuthState(
    val phoneNumber: String = "",
    val otpCode: String = "",
    val name: String = "",
    val email: String = "",
    val selectedCommunity: String = "",
    val flatNo: String = "",
    val selectedRole: String = "Resident",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isOtpSent: Boolean = false,
    val currentUser: AuthUser? = null,
    val isRegistered: Boolean = false,
    val isCommunitySelected: Boolean = false,
    val isVerificationSubmitted: Boolean = false
)
