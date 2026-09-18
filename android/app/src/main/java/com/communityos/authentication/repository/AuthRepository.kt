package com.communityos.authentication.repository

import com.communityos.authentication.model.AuthUser

interface AuthRepository {
    suspend fun sendOtp(phoneNumber: String): Result<Unit>
    suspend fun verifyOtp(code: String): Result<AuthUser>
    suspend fun registerUser(name: String, email: String): Result<AuthUser>
    suspend fun selectCommunity(communityName: String): Result<Unit>
    suspend fun verifyFlat(flatNo: String, role: String): Result<Unit>
    suspend fun restoreSession(): Result<AuthUser?>
    suspend fun logout(): Result<Unit>
}
