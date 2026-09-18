package com.communityos.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.communityos.profile.domain.GetProfileUseCase
import com.communityos.profile.domain.UpdateProfileUseCase
import com.communityos.profile.event.ProfileEvent
import com.communityos.profile.state.ProfileState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
    }

    init {
        onEvent(ProfileEvent.LoadProfile)
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            ProfileEvent.LoadProfile -> loadProfile()
            ProfileEvent.StartEditing -> {
                _state.update {
                    it.copy(
                        isEditing = true,
                        editName = it.profile?.name ?: "",
                        editEmail = it.profile?.email ?: "",
                        nameError = null,
                        emailError = null,
                        errorMessage = null,
                        successMessage = null
                    )
                }
            }
            ProfileEvent.CancelEditing -> {
                _state.update {
                    it.copy(
                        isEditing = false,
                        editName = "",
                        editEmail = "",
                        nameError = null,
                        emailError = null,
                        errorMessage = null
                    )
                }
            }
            is ProfileEvent.NameChanged -> {
                _state.update { it.copy(editName = event.name, nameError = null) }
            }
            is ProfileEvent.EmailChanged -> {
                _state.update { it.copy(editEmail = event.email, emailError = null) }
            }
            ProfileEvent.SaveProfile -> saveProfile()
            ProfileEvent.ClearMessages -> {
                _state.update { it.copy(errorMessage = null, successMessage = null) }
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = getProfileUseCase()
            result.fold(
                onSuccess = { profile ->
                    _state.update { it.copy(isLoading = false, profile = profile) }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            )
        }
    }

    private fun saveProfile() {
        val trimmedName = _state.value.editName.trim()
        val nameError = if (trimmedName.isBlank()) "Name cannot be blank" else null

        val trimmedEmail = _state.value.editEmail.trim()
        val emailError = if (trimmedEmail.isNotEmpty() && !trimmedEmail.matches(EMAIL_REGEX)) {
            "Invalid email format"
        } else {
            null
        }

        if (nameError != null || emailError != null) {
            _state.update { it.copy(nameError = nameError, emailError = emailError) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            val result = updateProfileUseCase(trimmedName, trimmedEmail.ifEmpty { null })
            result.fold(
                onSuccess = { updatedProfile ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            isEditing = false,
                            profile = updatedProfile,
                            editName = "",
                            editEmail = "",
                            nameError = null,
                            emailError = null,
                            successMessage = "Profile updated successfully"
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Failed to update profile"
                        )
                    }
                }
            )
        }
    }
}
