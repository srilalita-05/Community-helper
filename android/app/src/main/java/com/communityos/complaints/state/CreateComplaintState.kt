package com.communityos.complaints.state

data class CreateComplaintState(
    val category: String = "",
    val description: String = "",
    val categoryError: String? = null,
    val descriptionError: String? = null,
    val generalError: String? = null,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false
)
