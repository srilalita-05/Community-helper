package com.communityos.maintenance.model

data class MaintenanceBill(
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
    val isPayable: Boolean
        get() = status == BillStatus.UNPAID || status == BillStatus.OVERDUE
}
