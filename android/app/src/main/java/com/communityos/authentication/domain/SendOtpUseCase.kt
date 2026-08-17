package com.communityos.authentication.domain

import com.communityos.authentication.repository.AuthRepository
import javax.inject.Inject

class SendOtpUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(phoneNumber: String): Result<Unit> {
        return repository.sendOtp(phoneNumber)
    }
}
