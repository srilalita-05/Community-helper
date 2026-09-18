package com.communityos.authentication.repository

import com.communityos.authentication.model.AuthUser
import com.communityos.authentication.model.toAuthUser
import com.communityos.data.local.dao.CommunityDao
import com.communityos.data.local.dao.FlatDao
import com.communityos.data.local.dao.UserDao
import com.communityos.data.local.entity.UserEntity
import com.communityos.data.local.session.Session
import com.communityos.data.local.session.SessionManager
import com.communityos.models.User
import com.communityos.models.UserRole
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val communityDao: CommunityDao,
    private val flatDao: FlatDao,
    private val sessionManager: SessionManager
) : AuthRepository {

    private var tempPhone: String = ""
    private var currentUser: User? = null

    override suspend fun sendOtp(phoneNumber: String): Result<Unit> {
        delay(1000) // Simulate network delay
        if (phoneNumber.length < 10) {
            return Result.failure(IllegalArgumentException("Invalid phone number format"))
        }
        tempPhone = phoneNumber.trim()
        return Result.success(Unit)
    }

    override suspend fun verifyOtp(code: String): Result<AuthUser> {
        delay(1000) // Simulate network delay
        if (code.length != 6) {
            return Result.failure(IllegalArgumentException("OTP code must be 6 digits"))
        }
        // Simulated mock OTP check
        if (code != "123456" && code != "000000") {
            return Result.failure(IllegalArgumentException("Incorrect OTP. Use 123456 or 000000"))
        }

        // Query Room to check if this user already exists
        val existingEntity = userDao.getUserByPhoneNumber(tempPhone)
        val userDomain: User
        val isNew: Boolean

        if (existingEntity != null) {
            // Existing user: reuse without duplicate creation
            userDomain = existingEntity.toDomain()
            isNew = false
        } else {
            // Create initial persistent demo user in Room
            isNew = !tempPhone.endsWith("0")
            val newEntity = UserEntity(
                id = "user_${tempPhone.hashCode().toUInt()}",
                phoneNumber = tempPhone,
                name = "Resident ${tempPhone.takeLast(4)}",
                email = null,
                role = UserRole.RESIDENT,
                communityId = null,
                flatId = null,
                isApproved = false,
                createdAt = System.currentTimeMillis()
            )
            userDao.insertUser(newEntity)
            userDomain = newEntity.toDomain()
        }

        // Persist active session in DataStore (userId + role)
        sessionManager.saveSession(Session(userId = userDomain.id, role = userDomain.role))
        currentUser = userDomain

        return Result.success(userDomain.toAuthUser(isNewUser = isNew))
    }

    override suspend fun registerUser(name: String, email: String): Result<AuthUser> {
        delay(800)
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Name cannot be empty"))
        }

        val activeUser = currentUser
            ?: sessionManager.getSession()?.let { session ->
                userDao.getUserById(session.userId)?.toDomain()
            }
            ?: return Result.failure(IllegalStateException("No active user session found for registration"))

        val updatedEntity = UserEntity(
            id = activeUser.id,
            phoneNumber = activeUser.phoneNumber,
            name = name.trim(),
            email = email.trim().ifBlank { null },
            role = activeUser.role,
            communityId = activeUser.communityId,
            flatId = activeUser.flatId,
            isApproved = activeUser.isApproved,
            createdAt = activeUser.createdAt
        )

        userDao.updateUser(updatedEntity)
        val updatedDomain = updatedEntity.toDomain()
        currentUser = updatedDomain

        return Result.success(updatedDomain.toAuthUser(isNewUser = false))
    }

    override suspend fun selectCommunity(communityName: String): Result<Unit> {
        delay(500)
        if (communityName.isBlank()) {
            return Result.failure(IllegalArgumentException("Community name cannot be blank"))
        }

        val activeUser = currentUser
            ?: sessionManager.getSession()?.let { session ->
                userDao.getUserById(session.userId)?.toDomain()
            }
            ?: return Result.failure(IllegalStateException("No active user session found"))

        // Find community in Room by name
        val allCommunities = communityDao.getAllCommunities()
        val community = allCommunities.find { it.name.equals(communityName.trim(), ignoreCase = true) }
            ?: return Result.failure(IllegalArgumentException("Community '$communityName' not found"))

        // Associate user with selected community in Room
        userDao.updateUserCommunity(activeUser.id, community.id)
        currentUser = activeUser.copy(communityId = community.id)

        return Result.success(Unit)
    }

    override suspend fun verifyFlat(flatNo: String, role: String): Result<Unit> {
        delay(800)
        if (flatNo.isBlank()) {
            return Result.failure(IllegalArgumentException("Flat number is required"))
        }

        val activeUser = currentUser
            ?: sessionManager.getSession()?.let { session ->
                userDao.getUserById(session.userId)?.toDomain()
            }
            ?: return Result.failure(IllegalStateException("No active user session found"))

        val selectedCommunityId = activeUser.communityId
            ?: return Result.failure(IllegalStateException("Please select a community before verifying your flat"))

        // Find flat strictly within the selected community (Correction 2)
        val flat = flatDao.getFlatByNumber(
            communityId = selectedCommunityId,
            flatNumber = flatNo.trim()
        ) ?: return Result.failure(
            IllegalArgumentException("Flat '$flatNo' does not belong to the selected community")
        )

        val parsedRole = UserRole.fromString(role)

        // Update user in Room with selected flat and role
        userDao.updateUserRole(activeUser.id, parsedRole)
        userDao.updateUserCommunityAndFlat(activeUser.id, selectedCommunityId, flat.id)

        // Update active session in DataStore
        sessionManager.saveSession(Session(userId = activeUser.id, role = parsedRole))

        currentUser = activeUser.copy(
            role = parsedRole,
            flatId = flat.id
        )

        return Result.success(Unit)
    }
}
