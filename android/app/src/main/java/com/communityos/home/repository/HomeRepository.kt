package com.communityos.home.repository

import com.communityos.home.model.DashboardSummary

interface HomeRepository {
    suspend fun getDashboardSummary(): Result<DashboardSummary>
    suspend fun triggerEmergencyAlert(type: String): Result<Unit>
}
