package com.communityos.authentication.domain

import com.communityos.authentication.model.AuthUser
import com.communityos.authentication.repository.AuthRepository
import javax.inject.Inject

class RestoreSessionUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Result<AuthUser?> {
        return repository.restoreSession()
    }
}
