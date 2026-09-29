package com.communityos.maintenance.model

enum class PaymentMethod(val displayName: String) {
    UPI_SIMULATED("Simulated UPI"),
    CARD_SIMULATED("Simulated Debit / Credit Card"),
    NET_BANKING_SIMULATED("Simulated Net Banking"),
    CASH_SIMULATED("Simulated Cash / Cheque");

    companion object {
        fun fromString(value: String): PaymentMethod {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UPI_SIMULATED
        }
    }
}
