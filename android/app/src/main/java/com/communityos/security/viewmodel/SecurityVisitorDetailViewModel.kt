package com.communityos.security.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.communityos.security.event.SecurityVisitorDetailEvent
import com.communityos.security.state.SecurityVisitorDetailState
import com.communityos.visitors.domain.CheckInVisitorUseCase
import com.communityos.visitors.domain.CheckOutVisitorUseCase
import com.communityos.visitors.domain.DenyVisitorUseCase
import com.communityos.visitors.domain.GetSecurityVisitorDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityVisitorDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getSecurityVisitorDetailsUseCase: GetSecurityVisitorDetailsUseCase,
    private val checkInVisitorUseCase: CheckInVisitorUseCase,
    private val checkOutVisitorUseCase: CheckOutVisitorUseCase,
    private val denyVisitorUseCase: DenyVisitorUseCase
) : ViewModel() {

    private val visitorId: String = checkNotNull(savedStateHandle["visitorId"]) {
        "visitorId must be supplied to SecurityVisitorDetailViewModel"
    }

    private val _state = MutableStateFlow(SecurityVisitorDetailState())
    val state: StateFlow<SecurityVisitorDetailState> = _state.asStateFlow()

    init {
        loadDetail()
    }

    fun onEvent(event: SecurityVisitorDetailEvent) {
        when (event) {
            is SecurityVisitorDetailEvent.LoadDetails -> loadDetail()
            is SecurityVisitorDetailEvent.CheckIn -> checkIn()
            is SecurityVisitorDetailEvent.CheckOut -> checkOut()
            is SecurityVisitorDetailEvent.Deny -> deny()
            is SecurityVisitorDetailEvent.ClearSuccess -> {
                _state.update { it.copy(actionSuccessMessage = null) }
            }
        }
    }

    private fun loadDetail() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = getSecurityVisitorDetailsUseCase(visitorId)
            result.fold(
                onSuccess = { detail ->
                    _state.update { it.copy(isLoading = false, detail = detail, error = null) }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(isLoading = false, error = error.message ?: "Failed to load visitor details")
                    }
                }
            )
        }
    }

    private fun checkIn() {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, error = null) }
            val result = checkInVisitorUseCase(visitorId)
            result.fold(
                onSuccess = { updatedVisitor ->
                    _state.update { current ->
                        val currentDetail = current.detail
                        current.copy(
                            isProcessing = false,
                            detail = currentDetail?.copy(visitor = updatedVisitor),
                            actionSuccessMessage = "Visitor successfully checked in",
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = error.message ?: "Failed to check in visitor"
                        )
                    }
                }
            )
        }
    }

    private fun checkOut() {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, error = null) }
            val result = checkOutVisitorUseCase(visitorId)
            result.fold(
                onSuccess = { updatedVisitor ->
                    _state.update { current ->
                        val currentDetail = current.detail
                        current.copy(
                            isProcessing = false,
                            detail = currentDetail?.copy(visitor = updatedVisitor),
                            actionSuccessMessage = "Visitor successfully checked out",
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = error.message ?: "Failed to check out visitor"
                        )
                    }
                }
            )
        }
    }

    private fun deny() {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, error = null) }
            val result = denyVisitorUseCase(visitorId)
            result.fold(
                onSuccess = { updatedVisitor ->
                    _state.update { current ->
                        val currentDetail = current.detail
                        current.copy(
                            isProcessing = false,
                            detail = currentDetail?.copy(visitor = updatedVisitor),
                            actionSuccessMessage = "Visitor entry denied",
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = error.message ?: "Failed to deny visitor"
                        )
                    }
                }
            )
        }
    }
}
