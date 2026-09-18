package com.communityos.home.repository

import com.communityos.data.local.dao.CommunityDao
import com.communityos.data.local.dao.FlatDao
import com.communityos.data.local.dao.UserDao
import com.communityos.data.local.session.SessionManager
import com.communityos.home.model.DashboardSummary
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val communityDao: CommunityDao,
    private val flatDao: FlatDao,
    private val sessionManager: SessionManager
) : HomeRepository {

    override suspend fun getDashboardSummary(): Result<DashboardSummary> {
        delay(600) // Simulate brief delay

        // 1. Obtain current active session
        val session = sessionManager.getSession()
            ?: return Result.failure(IllegalStateException("No active session found"))

        // 2. Obtain user profile from Room
        val user = userDao.getUserById(session.userId)
            ?: return Result.failure(IllegalStateException("User not found for active session"))

        // 3. Resolve community from Room if associated
        val community = user.communityId?.let { communityDao.getCommunityById(it) }

        // 4. Resolve flat from Room if associated
        val flat = user.flatId?.let { flatDao.getFlatById(it) }

        // 5. Construct DashboardSummary using persisted data and safe fallbacks
        val summary = DashboardSummary(
            activeVisitors = 2,
            pendingComplaints = 1,
            outstandingDues = 120.50,
            communityName = community?.name ?: "Welcome Home",
            blockNo = flat?.block ?: "",
            flatNo = flat?.flatNumber ?: ""
        )

        return Result.success(summary)
    }

    override suspend fun triggerEmergencyAlert(type: String): Result<Unit> {
        delay(800) // Simulate network dispatch
        return Result.success(Unit)
    }
}
