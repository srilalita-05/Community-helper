package com.communityos.complaints.state

import com.communityos.complaints.model.Complaint

data class ComplaintDetailState(
    val isLoading: Boolean = false,
    val complaint: Complaint? = null,
    val error: String? = null
)
