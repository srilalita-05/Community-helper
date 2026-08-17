package com.communityos.home.state

import com.communityos.home.model.DashboardSummary

data class HomeState(
    val summary: DashboardSummary? = null,
    val isLoading: Boolean = false,
    val isEmergencyTriggered: Boolean = false,
    val emergencyType: String? = null,
    val errorMessage: String? = null
)
