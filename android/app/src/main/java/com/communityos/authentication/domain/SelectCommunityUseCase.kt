package com.communityos.authentication.domain

import com.communityos.authentication.repository.AuthRepository
import javax.inject.Inject

class SelectCommunityUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(communityName: String): Result<Unit> {
        return repository.selectCommunity(communityName)
    }
}
