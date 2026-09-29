package com.communityos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.communityos.maintenance.model.MaintenancePayment
import com.communityos.maintenance.model.PaymentMethod

@Entity(
    tableName = "maintenance_payments",
    indices = [
        Index(value = ["billId"]),
        Index(value = ["flatId"]),
        Index(value = ["residentId"])
    ]
)
data class MaintenancePaymentEntity(
    @PrimaryKey
    val id: String,
    val billId: String,
    val flatId: String,
    val residentId: String,
    val amountPaid: Double,
    val paymentMethod: PaymentMethod,
    val transactionRef: String,
    val paymentDate: Long
) {
    fun toDomain(): MaintenancePayment {
        return MaintenancePayment(
            id = id,
            billId = billId,
            flatId = flatId,
            residentId = residentId,
            amountPaid = amountPaid,
            paymentMethod = paymentMethod,
            transactionRef = transactionRef,
            paymentDate = paymentDate
        )
    }

    companion object {
        fun fromDomain(domain: MaintenancePayment): MaintenancePaymentEntity {
            return MaintenancePaymentEntity(
                id = domain.id,
                billId = domain.billId,
                flatId = domain.flatId,
                residentId = domain.residentId,
                amountPaid = domain.amountPaid,
                paymentMethod = domain.paymentMethod,
                transactionRef = domain.transactionRef,
                paymentDate = domain.paymentDate
            )
        }
    }
}
