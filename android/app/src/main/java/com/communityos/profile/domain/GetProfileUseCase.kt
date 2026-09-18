package com.communityos.profile.domain

import com.communityos.profile.model.UserProfile
import com.communityos.profile.repository.ProfileRepository
import javax.inject.Inject

class GetProfileUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(): Result<UserProfile> {
        return repository.getProfile()
    }
}
