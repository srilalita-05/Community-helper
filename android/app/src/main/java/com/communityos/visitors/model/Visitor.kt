package com.communityos.visitors.model

data class Visitor(
    val id: String,
    val residentId: String,
    val communityId: String,
    val flatId: String,
    val name: String,
    val phoneNumber: String,
    val purpose: String,
    val vehicleNumber: String? = null,
    val scheduledArrivalDate: Long,
    val status: VisitorStatus = VisitorStatus.PRE_APPROVED,
    val photoUri: String? = null,
    val checkInTime: Long? = null,
    val checkOutTime: Long? = null,
    val verifiedBySecurityId: String? = null,
    val createdAt: Long,
    val updatedAt: Long? = null
)
