package com.communityos.authentication.domain

import com.communityos.authentication.model.AuthUser
import com.communityos.authentication.repository.AuthRepository
import javax.inject.Inject

class VerifyOtpUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(code: String): Result<AuthUser> {
        return repository.verifyOtp(code)
    }
}
