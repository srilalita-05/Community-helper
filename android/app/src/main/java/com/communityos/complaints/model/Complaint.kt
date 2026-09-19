package com.communityos.complaints.model

data class Complaint(
    val id: String,
    val residentId: String,
    val communityId: String,
    val flatId: String,
    val category: String,
    val description: String,
    val status: ComplaintStatus,
    val createdAt: Long,
    val updatedAt: Long? = null
)
