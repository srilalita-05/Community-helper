package com.communityos.complaints.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.communityos.complaints.domain.GetComplaintDetailsUseCase
import com.communityos.complaints.event.ComplaintDetailEvent
import com.communityos.complaints.state.ComplaintDetailState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ComplaintDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getComplaintDetailsUseCase: GetComplaintDetailsUseCase
) : ViewModel() {

    private val complaintId: String = checkNotNull(savedStateHandle["complaintId"]) {
        "complaintId must be supplied to ComplaintDetailViewModel"
    }

    private val _state = MutableStateFlow(ComplaintDetailState())
    val state: StateFlow<ComplaintDetailState> = _state.asStateFlow()

    init {
        loadDetail()
    }

    fun onEvent(event: ComplaintDetailEvent) {
        when (event) {
            is ComplaintDetailEvent.LoadDetail,
            is ComplaintDetailEvent.Refresh -> loadDetail()
        }
    }

    private fun loadDetail() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = getComplaintDetailsUseCase(complaintId)
            result.fold(
                onSuccess = { complaint ->
                    _state.update { it.copy(isLoading = false, complaint = complaint, error = null) }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message ?: "Failed to load complaint details") }
                }
            )
        }
    }
}
