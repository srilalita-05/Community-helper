package com.communityos.complaints.state

import com.communityos.complaints.model.Complaint

data class ComplaintsListState(
    val isLoading: Boolean = false,
    val complaints: List<Complaint> = emptyList(),
    val error: String? = null
)
