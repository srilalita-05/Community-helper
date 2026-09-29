package com.communityos.maintenance.domain

import com.communityos.maintenance.model.MaintenancePaymentReceipt
import com.communityos.maintenance.model.PaymentMethod
import com.communityos.maintenance.repository.MaintenanceRepository
import javax.inject.Inject

class PayMaintenanceBillUseCase @Inject constructor(
    private val repository: MaintenanceRepository
) {
    suspend operator fun invoke(
        billId: String,
        paymentMethod: PaymentMethod
    ): Result<MaintenancePaymentReceipt> {
        return repository.payBill(billId, paymentMethod)
    }
}
