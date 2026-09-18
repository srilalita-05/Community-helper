package com.communityos.profile.state

import com.communityos.profile.model.UserProfile

data class ProfileState(
    val profile: UserProfile? = null,
    val isEditing: Boolean = false,
    val editName: String = "",
    val editEmail: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
