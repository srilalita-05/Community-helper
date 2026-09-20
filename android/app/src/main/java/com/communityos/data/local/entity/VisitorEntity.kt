package com.communityos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.communityos.visitors.model.Visitor
import com.communityos.visitors.model.VisitorStatus

@Entity(
    tableName = "visitors",
    indices = [
        Index(value = ["residentId"]),
        Index(value = ["communityId"]),
        Index(value = ["flatId"]),
        Index(value = ["status"]),
        Index(value = ["communityId", "status"])
    ]
)
data class VisitorEntity(
    @PrimaryKey
    val id: String,
    val residentId: String,
    val communityId: String,
    val flatId: String,
    val name: String,
    val phoneNumber: String,
    val purpose: String,
    val vehicleNumber: String? = null,
    val scheduledArrivalDate: Long,
    val status: VisitorStatus,
    val photoUri: String? = null,
    val checkInTime: Long? = null,
    val checkOutTime: Long? = null,
    val verifiedBySecurityId: String? = null,
    val createdAt: Long,
    val updatedAt: Long? = null
) {
    fun toDomain(): Visitor = Visitor(
        id = id,
        residentId = residentId,
        communityId = communityId,
        flatId = flatId,
        name = name,
        phoneNumber = phoneNumber,
        purpose = purpose,
        vehicleNumber = vehicleNumber,
        scheduledArrivalDate = scheduledArrivalDate,
        status = status,
        photoUri = photoUri,
        checkInTime = checkInTime,
        checkOutTime = checkOutTime,
        verifiedBySecurityId = verifiedBySecurityId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(visitor: Visitor): VisitorEntity = VisitorEntity(
            id = visitor.id,
            residentId = visitor.residentId,
            communityId = visitor.communityId,
            flatId = visitor.flatId,
            name = visitor.name,
            phoneNumber = visitor.phoneNumber,
            purpose = visitor.purpose,
            vehicleNumber = visitor.vehicleNumber,
            scheduledArrivalDate = visitor.scheduledArrivalDate,
            status = visitor.status,
            photoUri = visitor.photoUri,
            checkInTime = visitor.checkInTime,
            checkOutTime = visitor.checkOutTime,
            verifiedBySecurityId = visitor.verifiedBySecurityId,
            createdAt = visitor.createdAt,
            updatedAt = visitor.updatedAt
        )
    }
}
