package com.communityos.visitors.state

data class CreateVisitorState(
    val name: String = "",
    val phoneNumber: String = "",
    val purpose: String = "Guest",
    val vehicleNumber: String = "",
    val scheduledArrivalDate: Long = System.currentTimeMillis(),
    val isSaving: Boolean = false,
    val nameError: String? = null,
    val phoneError: String? = null,
    val purposeError: String? = null,
    val generalError: String? = null,
    val isSuccess: Boolean = false
)
