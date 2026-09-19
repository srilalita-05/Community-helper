package com.communityos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.communityos.complaints.model.Complaint
import com.communityos.complaints.model.ComplaintStatus

@Entity(
    tableName = "complaints",
    indices = [
        Index(value = ["residentId"]),
        Index(value = ["communityId"]),
        Index(value = ["flatId"])
    ]
)
data class ComplaintEntity(
    @PrimaryKey
    val id: String,
    val residentId: String,
    val communityId: String,
    val flatId: String,
    val category: String,
    val description: String,
    val status: ComplaintStatus,
    val createdAt: Long,
    val updatedAt: Long? = null
) {
    fun toDomain(): Complaint = Complaint(
        id = id,
        residentId = residentId,
        communityId = communityId,
        flatId = flatId,
        category = category,
        description = description,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(complaint: Complaint): ComplaintEntity = ComplaintEntity(
            id = complaint.id,
            residentId = complaint.residentId,
            communityId = complaint.communityId,
            flatId = complaint.flatId,
            category = complaint.category,
            description = complaint.description,
            status = complaint.status,
            createdAt = complaint.createdAt,
            updatedAt = complaint.updatedAt
        )
    }
}
