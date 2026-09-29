package com.communityos.maintenance.state

import com.communityos.maintenance.model.MaintenanceBill
import com.communityos.maintenance.model.MaintenancePayment

enum class MaintenanceTab {
    PENDING,
    HISTORY,
    PAYMENTS
}

data class MaintenanceDashboardState(
    val isLoading: Boolean = true,
    val outstandingTotal: Double = 0.0,
    val pendingBills: List<MaintenanceBill> = emptyList(),
    val billHistory: List<MaintenanceBill> = emptyList(),
    val payments: List<MaintenancePayment> = emptyList(),
    val selectedTab: MaintenanceTab = MaintenanceTab.PENDING,
    val error: String? = null
)
