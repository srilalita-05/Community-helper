package com.communityos.complaints.event

sealed class CreateComplaintEvent {
    data class OnCategoryChanged(val category: String) : CreateComplaintEvent()
    data class OnDescriptionChanged(val description: String) : CreateComplaintEvent()
    object Submit : CreateComplaintEvent()
    object ResetSuccess : CreateComplaintEvent()
}
