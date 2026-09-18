package com.communityos.profile.domain

import com.communityos.profile.model.UserProfile
import com.communityos.profile.repository.ProfileRepository
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(name: String, email: String?): Result<UserProfile> {
        return repository.updateProfile(name, email)
    }
}
