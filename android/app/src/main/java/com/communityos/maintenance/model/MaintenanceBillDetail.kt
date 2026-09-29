package com.communityos.maintenance.model

data class MaintenanceBillDetail(
    val bill: MaintenanceBill,
    val payment: MaintenancePayment? = null,
    val flatNumber: String = "",
    val block: String = "",
    val communityName: String = ""
)
