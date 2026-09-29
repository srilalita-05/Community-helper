package com.communityos.maintenance.event

import com.communityos.maintenance.state.MaintenanceTab

sealed class MaintenanceDashboardEvent {
    object Refresh : MaintenanceDashboardEvent()
    data class SelectTab(val tab: MaintenanceTab) : MaintenanceDashboardEvent()
    object DismissError : MaintenanceDashboardEvent()
}
