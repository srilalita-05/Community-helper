package com.communityos.profile.repository

import com.communityos.data.local.dao.CommunityDao
import com.communityos.data.local.dao.FlatDao
import com.communityos.data.local.dao.UserDao
import com.communityos.data.local.session.SessionManager
import com.communityos.profile.model.UserProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val communityDao: CommunityDao,
    private val flatDao: FlatDao,
    private val sessionManager: SessionManager
) : ProfileRepository {

    override suspend fun getProfile(): Result<UserProfile> {
        val session = sessionManager.getSession()
            ?: return Result.failure(IllegalStateException("No active session found"))

        val user = userDao.getUserById(session.userId)
            ?: return Result.failure(IllegalStateException("User not found for active session"))

        val community = user.communityId?.let { communityDao.getCommunityById(it) }
        val flat = user.flatId?.let { flatDao.getFlatById(it) }

        val profile = UserProfile(
            id = user.id,
            name = user.name,
            phoneNumber = user.phoneNumber,
            email = user.email,
            communityName = community?.name ?: "",
            flatNumber = flat?.flatNumber ?: "",
            role = user.role
        )

        return Result.success(profile)
    }

    override suspend fun updateProfile(name: String, email: String?): Result<UserProfile> {
        val session = sessionManager.getSession()
            ?: return Result.failure(IllegalStateException("No active session found"))

        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Name cannot be blank"))
        }

        val cleanedEmail = email?.trim()?.ifBlank { null }
        if (cleanedEmail != null) {
            val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
            if (!cleanedEmail.matches(emailPattern.toRegex())) {
                return Result.failure(IllegalArgumentException("Invalid email format"))
            }
        }

        userDao.updateUserNameAndEmail(
            userId = session.userId,
            name = trimmedName,
            email = cleanedEmail
        )

        return getProfile()
    }
}
