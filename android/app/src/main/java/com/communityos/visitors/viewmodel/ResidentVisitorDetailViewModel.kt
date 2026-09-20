package com.communityos.visitors.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.communityos.visitors.domain.CancelVisitorUseCase
import com.communityos.visitors.domain.GetResidentVisitorDetailsUseCase
import com.communityos.visitors.event.ResidentVisitorDetailEvent
import com.communityos.visitors.state.ResidentVisitorDetailState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResidentVisitorDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getResidentVisitorDetailsUseCase: GetResidentVisitorDetailsUseCase,
    private val cancelVisitorUseCase: CancelVisitorUseCase
) : ViewModel() {

    private val visitorId: String = checkNotNull(savedStateHandle["visitorId"]) {
        "visitorId must be supplied to ResidentVisitorDetailViewModel"
    }

    private val _state = MutableStateFlow(ResidentVisitorDetailState())
    val state: StateFlow<ResidentVisitorDetailState> = _state.asStateFlow()

    init {
        loadDetail()
    }

    fun onEvent(event: ResidentVisitorDetailEvent) {
        when (event) {
            is ResidentVisitorDetailEvent.LoadDetails -> loadDetail()
            is ResidentVisitorDetailEvent.CancelPass -> cancelPass()
            is ResidentVisitorDetailEvent.ResetCancelSuccess -> {
                _state.update { it.copy(cancelSuccess = false) }
            }
        }
    }

    private fun loadDetail() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = getResidentVisitorDetailsUseCase(visitorId)
            result.fold(
                onSuccess = { visitor ->
                    _state.update { it.copy(isLoading = false, visitor = visitor, error = null) }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(isLoading = false, error = error.message ?: "Failed to load visitor details")
                    }
                }
            )
        }
    }

    private fun cancelPass() {
        viewModelScope.launch {
            _state.update { it.copy(isCancelling = true, error = null) }
            val result = cancelVisitorUseCase(visitorId)
            result.fold(
                onSuccess = { updatedVisitor ->
                    _state.update {
                        it.copy(
                            isCancelling = false,
                            visitor = updatedVisitor,
                            cancelSuccess = true,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isCancelling = false,
                            error = error.message ?: "Failed to cancel visitor pass"
                        )
                    }
                }
            )
        }
    }
}
