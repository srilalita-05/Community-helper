package com.communityos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.communityos.maintenance.model.BillStatus
import com.communityos.maintenance.model.MaintenanceBill

@Entity(
    tableName = "maintenance_bills",
    indices = [
        Index(value = ["flatId"]),
        Index(value = ["communityId"]),
        Index(value = ["status"]),
        Index(value = ["flatId", "status"])
    ]
)
data class MaintenanceBillEntity(
    @PrimaryKey
    val id: String,
    val flatId: String,
    val communityId: String,
    val title: String,
    val period: String,
    val amount: Double,
    val dueDate: Long,
    val status: BillStatus,
    val createdAt: Long,
    val updatedAt: Long? = null
) {
    fun toDomain(): MaintenanceBill {
        return MaintenanceBill(
            id = id,
            flatId = flatId,
            communityId = communityId,
            title = title,
            period = period,
            amount = amount,
            dueDate = dueDate,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(domain: MaintenanceBill): MaintenanceBillEntity {
            return MaintenanceBillEntity(
                id = domain.id,
                flatId = domain.flatId,
                communityId = domain.communityId,
                title = domain.title,
                period = domain.period,
                amount = domain.amount,
                dueDate = domain.dueDate,
                status = domain.status,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt
            )
        }
    }
}
