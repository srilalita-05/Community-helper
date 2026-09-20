package com.communityos.visitors.repository

import com.communityos.data.local.dao.CommunityDao
import com.communityos.data.local.dao.FlatDao
import com.communityos.data.local.dao.UserDao
import com.communityos.data.local.dao.VisitorDao
import com.communityos.data.local.entity.UserEntity
import com.communityos.data.local.entity.VisitorEntity
import com.communityos.data.local.session.SessionManager
import com.communityos.models.UserRole
import com.communityos.visitors.model.SecurityVisitorDetail
import com.communityos.visitors.model.Visitor
import com.communityos.visitors.model.VisitorStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VisitorRepositoryImpl @Inject constructor(
    private val visitorDao: VisitorDao,
    private val userDao: UserDao,
    private val flatDao: FlatDao,
    private val communityDao: CommunityDao,
    private val sessionManager: SessionManager
) : VisitorRepository {

    private suspend fun getAuthenticatedResident(): Result<UserEntity> {
        val session = sessionManager.getSession()
            ?: return Result.failure(IllegalStateException("No active session found"))

        val user = userDao.getUserById(session.userId)
            ?: return Result.failure(IllegalStateException("User not found for active session"))

        if (user.role != UserRole.RESIDENT) {
            return Result.failure(SecurityException("User is not authorized as a resident"))
        }

        if (user.communityId.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Resident is not associated with any community"))
        }

        if (user.flatId.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Resident is not associated with any flat"))
        }

        return Result.success(user)
    }

    private suspend fun getAuthenticatedSecurity(): Result<UserEntity> {
        val session = sessionManager.getSession()
            ?: return Result.failure(IllegalStateException("No active session found"))

        val user = userDao.getUserById(session.userId)
            ?: return Result.failure(IllegalStateException("User not found for active session"))

        if (user.role != UserRole.SECURITY) {
            return Result.failure(SecurityException("User is not authorized as security personnel"))
        }

        if (user.communityId.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Security personnel is not associated with any community"))
        }

        return Result.success(user)
    }

    override suspend fun getMyVisitors(): Result<List<Visitor>> {
        val residentResult = getAuthenticatedResident()
        if (residentResult.isFailure) {
            return Result.failure(residentResult.exceptionOrNull()!!)
        }
        val resident = residentResult.getOrThrow()

        val entities = visitorDao.getVisitorsForResident(resident.id)
        return Result.success(entities.map { it.toDomain() })
    }

    override fun observeMyVisitors(): Flow<List<Visitor>> = flow {
        val residentResult = getAuthenticatedResident()
        if (residentResult.isSuccess) {
            val resident = residentResult.getOrThrow()
            emitAll(
                visitorDao.observeVisitorsForResident(resident.id)
                    .map { list -> list.map { it.toDomain() } }
            )
        } else {
            emit(emptyList())
        }
    }

    override suspend fun getResidentVisitorDetails(visitorId: String): Result<Visitor> {
        val residentResult = getAuthenticatedResident()
        if (residentResult.isFailure) {
            return Result.failure(residentResult.exceptionOrNull()!!)
        }
        val resident = residentResult.getOrThrow()

        val entity = visitorDao.getVisitorByIdAndResident(
            visitorId = visitorId,
            residentId = resident.id
        ) ?: return Result.failure(NoSuchElementException("Visitor not found for resident"))

        return Result.success(entity.toDomain())
    }

    override suspend fun createVisitor(
        name: String,
        phoneNumber: String,
        purpose: String,
        scheduledArrivalDate: Long,
        vehicleNumber: String?
    ): Result<Visitor> {
        val residentResult = getAuthenticatedResident()
        if (residentResult.isFailure) {
            return Result.failure(residentResult.exceptionOrNull()!!)
        }
        val resident = residentResult.getOrThrow()

        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Visitor name cannot be blank"))
        }

        val trimmedPhone = phoneNumber.trim()
        val phoneRegex = Regex("^[6-9]\\d{9}$")
        if (!trimmedPhone.matches(phoneRegex)) {
            return Result.failure(IllegalArgumentException("Invalid phone number. Must be a valid 10-digit number"))
        }

        val trimmedPurpose = purpose.trim()
        if (trimmedPurpose.isBlank()) {
            return Result.failure(IllegalArgumentException("Visit purpose cannot be blank"))
        }

        if (scheduledArrivalDate <= 0L) {
            return Result.failure(IllegalArgumentException("Invalid scheduled arrival date"))
        }

        val now = System.currentTimeMillis()
        val visitorId = "visitor_${UUID.randomUUID()}"

        val entity = VisitorEntity(
            id = visitorId,
            residentId = resident.id,
            communityId = resident.communityId!!,
            flatId = resident.flatId!!,
            name = trimmedName,
            phoneNumber = trimmedPhone,
            purpose = trimmedPurpose,
            vehicleNumber = vehicleNumber?.trim()?.ifBlank { null },
            scheduledArrivalDate = scheduledArrivalDate,
            status = VisitorStatus.PRE_APPROVED,
            photoUri = null,
            checkInTime = null,
            checkOutTime = null,
            verifiedBySecurityId = null,
            createdAt = now,
            updatedAt = null
        )

        visitorDao.insertVisitor(entity)
        return Result.success(entity.toDomain())
    }

    override suspend fun cancelVisitor(visitorId: String): Result<Visitor> {
        val residentResult = getAuthenticatedResident()
        if (residentResult.isFailure) {
            return Result.failure(residentResult.exceptionOrNull()!!)
        }
        val resident = residentResult.getOrThrow()

        val entity = visitorDao.getVisitorByIdAndResident(
            visitorId = visitorId,
            residentId = resident.id
        ) ?: return Result.failure(NoSuchElementException("Visitor not found for resident"))

        if (entity.status != VisitorStatus.PRE_APPROVED) {
            return Result.failure(
                IllegalStateException("Cannot cancel visitor with status ${entity.status}. Only PRE_APPROVED visitors can be cancelled.")
            )
        }

        val now = System.currentTimeMillis()
        val updated = entity.copy(
            status = VisitorStatus.CANCELLED,
            updatedAt = now
        )

        visitorDao.updateVisitor(updated)
        return Result.success(updated.toDomain())
    }

    override suspend fun getActiveVisitorsCount(): Result<Int> {
        val residentResult = getAuthenticatedResident()
        if (residentResult.isFailure) {
            return Result.failure(residentResult.exceptionOrNull()!!)
        }
        val resident = residentResult.getOrThrow()

        val count = visitorDao.countActiveVisitorsForResident(resident.id)
        return Result.success(count)
    }

    override suspend fun getSecurityGateProfile(): Result<com.communityos.visitors.model.SecurityGateProfile> {
        val securityResult = getAuthenticatedSecurity()
        if (securityResult.isFailure) {
            return Result.failure(securityResult.exceptionOrNull()!!)
        }
        val security = securityResult.getOrThrow()
        val community = communityDao.getCommunityById(security.communityId!!)

        return Result.success(
            com.communityos.visitors.model.SecurityGateProfile(
                officerName = security.name.ifBlank { "Security Officer" },
                communityName = community?.name ?: "Community Gate"
            )
        )
    }

    override suspend fun getSecurityVisitors(): Result<List<Visitor>> {
        val securityResult = getAuthenticatedSecurity()
        if (securityResult.isFailure) {
            return Result.failure(securityResult.exceptionOrNull()!!)
        }
        val security = securityResult.getOrThrow()

        val entities = visitorDao.getVisitorsForCommunity(security.communityId!!)
        return Result.success(entities.map { it.toDomain() })
    }

    override fun observeSecurityVisitors(): Flow<List<Visitor>> = flow {
        val securityResult = getAuthenticatedSecurity()
        if (securityResult.isSuccess) {
            val security = securityResult.getOrThrow()
            emitAll(
                visitorDao.observeVisitorsForCommunity(security.communityId!!)
                    .map { list -> list.map { it.toDomain() } }
            )
        } else {
            emit(emptyList())
        }
    }

    override suspend fun getSecurityVisitorDetails(visitorId: String): Result<SecurityVisitorDetail> {
        val securityResult = getAuthenticatedSecurity()
        if (securityResult.isFailure) {
            return Result.failure(securityResult.exceptionOrNull()!!)
        }
        val security = securityResult.getOrThrow()

        val entity = visitorDao.getVisitorByIdAndCommunity(
            visitorId = visitorId,
            communityId = security.communityId!!
        ) ?: return Result.failure(NoSuchElementException("Visitor not found for community"))

        val hostResident = userDao.getUserById(entity.residentId)
        val flat = flatDao.getFlatById(entity.flatId)
        val community = communityDao.getCommunityById(entity.communityId)

        val detail = SecurityVisitorDetail(
            visitor = entity.toDomain(),
            residentName = hostResident?.name ?: "Resident",
            residentPhone = hostResident?.phoneNumber ?: "",
            flatNumber = flat?.flatNumber ?: "N/A",
            block = flat?.block ?: "N/A",
            communityName = community?.name ?: "Community"
        )

        return Result.success(detail)
    }

    override suspend fun checkInVisitor(visitorId: String): Result<Visitor> {
        val securityResult = getAuthenticatedSecurity()
        if (securityResult.isFailure) {
            return Result.failure(securityResult.exceptionOrNull()!!)
        }
        val security = securityResult.getOrThrow()

        val entity = visitorDao.getVisitorByIdAndCommunity(
            visitorId = visitorId,
            communityId = security.communityId!!
        ) ?: return Result.failure(NoSuchElementException("Visitor not found for community"))

        if (entity.status != VisitorStatus.PRE_APPROVED) {
            return Result.failure(
                IllegalStateException("Cannot check in visitor with status ${entity.status}. Only PRE_APPROVED visitors can be checked in.")
            )
        }

        val now = System.currentTimeMillis()
        val updated = entity.copy(
            status = VisitorStatus.CHECKED_IN,
            checkInTime = now,
            verifiedBySecurityId = security.id,
            updatedAt = now
        )

        visitorDao.updateVisitor(updated)
        return Result.success(updated.toDomain())
    }

    override suspend fun checkOutVisitor(visitorId: String): Result<Visitor> {
        val securityResult = getAuthenticatedSecurity()
        if (securityResult.isFailure) {
            return Result.failure(securityResult.exceptionOrNull()!!)
        }
        val security = securityResult.getOrThrow()

        val entity = visitorDao.getVisitorByIdAndCommunity(
            visitorId = visitorId,
            communityId = security.communityId!!
        ) ?: return Result.failure(NoSuchElementException("Visitor not found for community"))

        if (entity.status != VisitorStatus.CHECKED_IN) {
            return Result.failure(
                IllegalStateException("Cannot check out visitor with status ${entity.status}. Only CHECKED_IN visitors can be checked out.")
            )
        }

        val now = System.currentTimeMillis()
        val updated = entity.copy(
            status = VisitorStatus.CHECKED_OUT,
            checkOutTime = now,
            updatedAt = now
        )

        visitorDao.updateVisitor(updated)
        return Result.success(updated.toDomain())
    }

    override suspend fun denyVisitor(visitorId: String): Result<Visitor> {
        val securityResult = getAuthenticatedSecurity()
        if (securityResult.isFailure) {
            return Result.failure(securityResult.exceptionOrNull()!!)
        }
        val security = securityResult.getOrThrow()

        val entity = visitorDao.getVisitorByIdAndCommunity(
            visitorId = visitorId,
            communityId = security.communityId!!
        ) ?: return Result.failure(NoSuchElementException("Visitor not found for community"))

        if (entity.status != VisitorStatus.PRE_APPROVED) {
            return Result.failure(
                IllegalStateException("Cannot deny entry for visitor with status ${entity.status}. Only PRE_APPROVED visitors can be denied.")
            )
        }

        val now = System.currentTimeMillis()
        val updated = entity.copy(
            status = VisitorStatus.DENIED,
            verifiedBySecurityId = security.id,
            updatedAt = now
        )

        visitorDao.updateVisitor(updated)
        return Result.success(updated.toDomain())
    }
}
