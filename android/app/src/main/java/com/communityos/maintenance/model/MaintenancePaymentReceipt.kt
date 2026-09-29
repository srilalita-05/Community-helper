package com.communityos.maintenance.model

data class MaintenancePaymentReceipt(
    val payment: MaintenancePayment,
    val bill: MaintenanceBill
)
