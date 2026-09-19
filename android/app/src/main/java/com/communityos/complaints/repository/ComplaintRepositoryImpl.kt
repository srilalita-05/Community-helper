package com.communityos.complaints.repository

import com.communityos.complaints.model.Complaint
import com.communityos.complaints.model.ComplaintStatus
import com.communityos.data.local.dao.ComplaintDao
import com.communityos.data.local.dao.UserDao
import com.communityos.data.local.entity.ComplaintEntity
import com.communityos.data.local.session.SessionManager
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComplaintRepositoryImpl @Inject constructor(
    private val complaintDao: ComplaintDao,
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) : ComplaintRepository {

    override suspend fun getMyComplaints(): Result<List<Complaint>> {
        val session = sessionManager.getSession()
            ?: return Result.failure(IllegalStateException("No active session found"))

        val entities = complaintDao.getComplaintsForResident(session.userId)
        return Result.success(entities.map { it.toDomain() })
    }

    override suspend fun getComplaintDetails(complaintId: String): Result<Complaint> {
        val session = sessionManager.getSession()
            ?: return Result.failure(IllegalStateException("No active session found"))

        val entity = complaintDao.getComplaintByIdAndResident(
            id = complaintId,
            residentId = session.userId
        ) ?: return Result.failure(NoSuchElementException("Complaint not found for active resident"))

        return Result.success(entity.toDomain())
    }

    override suspend fun createComplaint(
        category: String,
        description: String
    ): Result<Complaint> {
        val trimmedCategory = category.trim()
        val trimmedDescription = description.trim()

        if (trimmedCategory.isBlank()) {
            return Result.failure(IllegalArgumentException("Category cannot be blank"))
        }

        if (trimmedDescription.isBlank()) {
            return Result.failure(IllegalArgumentException("Description cannot be blank"))
        }

        val session = sessionManager.getSession()
            ?: return Result.failure(IllegalStateException("No active session found"))

        val user = userDao.getUserById(session.userId)
            ?: return Result.failure(IllegalStateException("User not found for active session"))

        val communityId = user.communityId
        if (communityId.isNullOrBlank()) {
            return Result.failure(IllegalStateException("User is not associated with any community"))
        }

        val flatId = user.flatId
        if (flatId.isNullOrBlank()) {
            return Result.failure(IllegalStateException("User is not associated with any flat"))
        }

        val complaintId = "complaint_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()

        val entity = ComplaintEntity(
            id = complaintId,
            residentId = session.userId,
            communityId = communityId,
            flatId = flatId,
            category = trimmedCategory,
            description = trimmedDescription,
            status = ComplaintStatus.SUBMITTED,
            createdAt = now,
            updatedAt = null
        )

        complaintDao.insertComplaint(entity)
        return Result.success(entity.toDomain())
    }

    override suspend fun getPendingComplaintsCount(): Result<Int> {
        val session = sessionManager.getSession()
            ?: return Result.failure(IllegalStateException("No active session found"))

        val count = complaintDao.getPendingComplaintsCount(session.userId)
        return Result.success(count)
    }
}
