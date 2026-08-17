package com.communityos.home.repository

import com.communityos.home.model.DashboardSummary
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepositoryImpl @Inject constructor() : HomeRepository {

    override suspend fun getDashboardSummary(): Result<DashboardSummary> {
        delay(1000) // Simulate network request delay
        // ponytail: simulated local mock data for Phase 3.
        // Upgrade path: integrate with Retrofit API client fetch.
        return Result.success(
            DashboardSummary(
                activeVisitors = 2,
                pendingComplaints = 1,
                outstandingDues = 120.50,
                communityName = "Orchard Heights Apartments",
                blockNo = "Block B",
                flatNo = "B-304"
            )
        )
    }

    override suspend fun triggerEmergencyAlert(type: String): Result<Unit> {
        delay(800) // Simulate network dispatch
        // ponytail: simulated notification dispatch for emergency alert.
        // Upgrade path: call Firebase FCM notification dispatch API endpoint.
        return Result.success(Unit)
    }
}
