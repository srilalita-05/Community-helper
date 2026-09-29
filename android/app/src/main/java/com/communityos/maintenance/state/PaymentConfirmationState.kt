package com.communityos.maintenance.state

import com.communityos.maintenance.model.MaintenanceBill
import com.communityos.maintenance.model.MaintenancePaymentReceipt
import com.communityos.maintenance.model.PaymentMethod

data class PaymentConfirmationState(
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
    val bill: MaintenanceBill? = null,
    val selectedMethod: PaymentMethod = PaymentMethod.UPI_SIMULATED,
    val receipt: MaintenancePaymentReceipt? = null,
    val isSuccess: Boolean = false,
    val error: String? = null
)
