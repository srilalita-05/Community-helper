package com.communityos.authentication.domain

import com.communityos.authentication.model.AuthUser
import com.communityos.authentication.repository.AuthRepository
import javax.inject.Inject

class RegisterUserUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(name: String, email: String): Result<AuthUser> {
        return repository.registerUser(name, email)
    }
}
