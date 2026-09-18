package com.communityos.profile.repository

import com.communityos.profile.model.UserProfile

interface ProfileRepository {
    suspend fun getProfile(): Result<UserProfile>
    suspend fun updateProfile(name: String, email: String?): Result<UserProfile>
}
