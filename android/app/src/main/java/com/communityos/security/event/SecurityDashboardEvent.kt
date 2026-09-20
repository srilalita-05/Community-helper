package com.communityos.security.event

import com.communityos.security.state.SecurityTab

sealed class SecurityDashboardEvent {
    object LoadDashboard : SecurityDashboardEvent()
    object Refresh : SecurityDashboardEvent()
    data class SelectTab(val tab: SecurityTab) : SecurityDashboardEvent()
    object Logout : SecurityDashboardEvent()
}
