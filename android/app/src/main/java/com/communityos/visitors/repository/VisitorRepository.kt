package com.communityos.visitors.repository

import com.communityos.visitors.model.SecurityVisitorDetail
import com.communityos.visitors.model.Visitor
import kotlinx.coroutines.flow.Flow

interface VisitorRepository {
    // Resident Operations
    suspend fun getMyVisitors(): Result<List<Visitor>>
    fun observeMyVisitors(): Flow<List<Visitor>>
    suspend fun getResidentVisitorDetails(visitorId: String): Result<Visitor>
    suspend fun createVisitor(
        name: String,
        phoneNumber: String,
        purpose: String,
        scheduledArrivalDate: Long,
        vehicleNumber: String? = null
    ): Result<Visitor>
    suspend fun cancelVisitor(visitorId: String): Result<Visitor>
    suspend fun getActiveVisitorsCount(): Result<Int>

    // Security Operations
    suspend fun getSecurityGateProfile(): Result<com.communityos.visitors.model.SecurityGateProfile>
    suspend fun getSecurityVisitors(): Result<List<Visitor>>
    fun observeSecurityVisitors(): Flow<List<Visitor>>
    suspend fun getSecurityVisitorDetails(visitorId: String): Result<SecurityVisitorDetail>
    suspend fun checkInVisitor(visitorId: String): Result<Visitor>
    suspend fun checkOutVisitor(visitorId: String): Result<Visitor>
    suspend fun denyVisitor(visitorId: String): Result<Visitor>
}
