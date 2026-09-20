package com.communityos.visitors.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.communityos.visitors.domain.CreateVisitorUseCase
import com.communityos.visitors.event.CreateVisitorEvent
import com.communityos.visitors.state.CreateVisitorState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateVisitorViewModel @Inject constructor(
    private val createVisitorUseCase: CreateVisitorUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CreateVisitorState())
    val state: StateFlow<CreateVisitorState> = _state.asStateFlow()

    fun onEvent(event: CreateVisitorEvent) {
        when (event) {
            is CreateVisitorEvent.OnNameChanged -> {
                _state.update { it.copy(name = event.name, nameError = null, generalError = null) }
            }
            is CreateVisitorEvent.OnPhoneChanged -> {
                _state.update { it.copy(phoneNumber = event.phone, phoneError = null, generalError = null) }
            }
            is CreateVisitorEvent.OnPurposeChanged -> {
                _state.update { it.copy(purpose = event.purpose, purposeError = null, generalError = null) }
            }
            is CreateVisitorEvent.OnVehicleChanged -> {
                _state.update { it.copy(vehicleNumber = event.vehicle, generalError = null) }
            }
            is CreateVisitorEvent.OnDateChanged -> {
                _state.update { it.copy(scheduledArrivalDate = event.date) }
            }
            is CreateVisitorEvent.Submit -> submitVisitor()
            is CreateVisitorEvent.ResetSuccess -> {
                _state.update { it.copy(isSuccess = false) }
            }
        }
    }

    private fun submitVisitor() {
        val current = _state.value
        val name = current.name.trim()
        val phone = current.phoneNumber.trim()
        val purpose = current.purpose.trim()
        val vehicle = current.vehicleNumber.trim()

        var hasError = false
        var nameErr: String? = null
        var phoneErr: String? = null
        var purposeErr: String? = null

        if (name.isBlank()) {
            nameErr = "Visitor name cannot be empty"
            hasError = true
        }

        val phoneRegex = Regex("^[6-9]\\d{9}$")
        if (phone.isBlank()) {
            phoneErr = "Phone number cannot be empty"
            hasError = true
        } else if (!phone.matches(phoneRegex)) {
            phoneErr = "Enter a valid 10-digit mobile number starting with 6-9"
            hasError = true
        }

        if (purpose.isBlank()) {
            purposeErr = "Purpose of visit cannot be empty"
            hasError = true
        }

        if (hasError) {
            _state.update {
                it.copy(
                    nameError = nameErr,
                    phoneError = phoneErr,
                    purposeError = purposeErr
                )
            }
            return
        }

        if (current.isSaving) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, generalError = null) }
            val result = createVisitorUseCase(
                name = name,
                phoneNumber = phone,
                purpose = purpose,
                scheduledArrivalDate = current.scheduledArrivalDate,
                vehicleNumber = vehicle.ifBlank { null }
            )
            result.fold(
                onSuccess = {
                    _state.update { it.copy(isSaving = false, isSuccess = true) }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            generalError = error.message ?: "Failed to create visitor pass"
                        )
                    }
                }
            )
        }
    }
}
