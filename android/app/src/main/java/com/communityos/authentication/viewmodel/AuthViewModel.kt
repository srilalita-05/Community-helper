package com.communityos.authentication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.communityos.authentication.domain.*
import com.communityos.authentication.event.AuthEvent
import com.communityos.authentication.state.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val sendOtpUseCase: SendOtpUseCase,
    private val verifyOtpUseCase: VerifyOtpUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
    private val selectCommunityUseCase: SelectCommunityUseCase,
    private val verifyFlatUseCase: VerifyFlatUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.OnPhoneChanged -> {
                _state.update { it.copy(phoneNumber = event.phone) }
            }
            is AuthEvent.OnOtpChanged -> {
                _state.update { it.copy(otpCode = event.otp) }
            }
            is AuthEvent.OnNameChanged -> {
                _state.update { it.copy(name = event.name) }
            }
            is AuthEvent.OnEmailChanged -> {
                _state.update { it.copy(email = event.email) }
            }
            is AuthEvent.OnCommunitySelected -> {
                _state.update { it.copy(selectedCommunity = event.community) }
                selectCommunity(event.community)
            }
            is AuthEvent.OnFlatNoChanged -> {
                _state.update { it.copy(flatNo = event.flatNo) }
            }
            is AuthEvent.OnRoleSelected -> {
                _state.update { it.copy(selectedRole = event.role) }
            }
            AuthEvent.SendOtp -> sendOtp()
            AuthEvent.VerifyOtp -> verifyOtp()
            AuthEvent.RegisterUser -> registerUser()
            AuthEvent.SubmitFlatVerification -> submitFlatVerification()
            AuthEvent.ClearError -> {
                _state.update { it.copy(errorMessage = null) }
            }
        }
    }

    private fun sendOtp() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = sendOtpUseCase(_state.value.phoneNumber)
            result.fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false, isOtpSent = true) }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            )
        }
    }

    private fun verifyOtp() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = verifyOtpUseCase(_state.value.otpCode)
            result.fold(
                onSuccess = { user ->
                    _state.update { it.copy(isLoading = false, currentUser = user) }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            )
        }
    }

    private fun registerUser() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = registerUserUseCase(_state.value.name, _state.value.email)
            result.fold(
                onSuccess = { user ->
                    _state.update { it.copy(isLoading = false, currentUser = user, isRegistered = true) }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            )
        }
    }

    private fun selectCommunity(name: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = selectCommunityUseCase(name)
            result.fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false, isCommunitySelected = true) }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            )
        }
    }

    private fun submitFlatVerification() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = verifyFlatUseCase(_state.value.flatNo, _state.value.selectedRole)
            result.fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false, isVerificationSubmitted = true) }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            )
        }
    }
}
