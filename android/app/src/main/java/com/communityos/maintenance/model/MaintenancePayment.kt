package com.communityos.maintenance.model

data class MaintenancePayment(
    val id: String,
    val billId: String,
    val flatId: String,
    val residentId: String,
    val amountPaid: Double,
    val paymentMethod: PaymentMethod,
    val transactionRef: String,
    val paymentDate: Long
)
