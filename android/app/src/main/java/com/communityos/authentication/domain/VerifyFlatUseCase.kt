package com.communityos.authentication.domain

import com.communityos.authentication.repository.AuthRepository
import javax.inject.Inject

class VerifyFlatUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(flatNo: String, role: String): Result<Unit> {
        return repository.verifyFlat(flatNo, role)
    }
}
