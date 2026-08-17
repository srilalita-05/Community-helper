package com.communityos.authentication.repository

import com.communityos.authentication.model.AuthUser
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor() : AuthRepository {
    private var tempPhone: String = ""
    private var currentUser: AuthUser? = null

    override suspend fun sendOtp(phoneNumber: String): Result<Unit> {
        delay(1000) // Simulate network delay
        if (phoneNumber.length < 10) {
            return Result.failure(IllegalArgumentException("Invalid phone number format"))
        }
        tempPhone = phoneNumber
        return Result.success(Unit)
    }

    override suspend fun verifyOtp(code: String): Result<AuthUser> {
        delay(1000) // Simulate network delay
        if (code.length != 6) {
            return Result.failure(IllegalArgumentException("OTP code must be 6 digits"))
        }
        // ponytail: simulated OTP check. Hardcode "123456" for verification.
        // Upgrade path: Integrate real Firebase Phone Authentication Callback.
        if (code != "123456" && code != "000000") {
            return Result.failure(IllegalArgumentException("Incorrect OTP. Use 123456 or 000000"))
        }
        val isNew = !tempPhone.endsWith("0") // Odd numbers/mock logic to differentiate new users
        val user = AuthUser(
            uid = "mock_uid_${tempPhone.hashCode()}",
            phoneNumber = tempPhone,
            isNewUser = isNew
        )
        currentUser = user
        return Result.success(user)
    }

    override suspend fun registerUser(name: String, email: String): Result<AuthUser> {
        delay(800)
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Name cannot be empty"))
        }
        val updatedUser = currentUser?.copy(displayName = name, email = email, isNewUser = false)
            ?: AuthUser("mock_uid", tempPhone, name, email, false)
        currentUser = updatedUser
        return Result.success(updatedUser)
    }

    override suspend fun selectCommunity(communityName: String): Result<Unit> {
        delay(500)
        return Result.success(Unit)
    }

    override suspend fun verifyFlat(flatNo: String, role: String): Result<Unit> {
        delay(800)
        if (flatNo.isBlank()) {
            return Result.failure(IllegalArgumentException("Flat number is required"))
        }
        return Result.success(Unit)
    }
}
