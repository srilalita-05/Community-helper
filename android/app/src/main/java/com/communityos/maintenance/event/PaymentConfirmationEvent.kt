package com.communityos.maintenance.event

import com.communityos.maintenance.model.PaymentMethod

sealed class PaymentConfirmationEvent {
    object Refresh : PaymentConfirmationEvent()
    data class SelectMethod(val method: PaymentMethod) : PaymentConfirmationEvent()
    object ConfirmPayment : PaymentConfirmationEvent()
    object DismissError : PaymentConfirmationEvent()
}

sealed class PaymentEffect {
    data class PaymentSuccess(val transactionRef: String) : PaymentEffect()
}
