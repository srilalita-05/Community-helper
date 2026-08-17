package com.communityos.authentication.event

sealed class AuthEvent {
    data class OnPhoneChanged(val phone: String) : AuthEvent()
    data class OnOtpChanged(val otp: String) : AuthEvent()
    data class OnNameChanged(val name: String) : AuthEvent()
    data class OnEmailChanged(val email: String) : AuthEvent()
    data class OnCommunitySelected(val community: String) : AuthEvent()
    data class OnFlatNoChanged(val flatNo: String) : AuthEvent()
    data class OnRoleSelected(val role: String) : AuthEvent()

    object SendOtp : AuthEvent()
    object VerifyOtp : AuthEvent()
    object RegisterUser : AuthEvent()
    object SubmitFlatVerification : AuthEvent()

    object ClearError : AuthEvent()
}
