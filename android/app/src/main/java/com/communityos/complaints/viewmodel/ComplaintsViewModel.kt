package com.communityos.complaints.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.communityos.complaints.domain.GetMyComplaintsUseCase
import com.communityos.complaints.event.ComplaintsListEvent
import com.communityos.complaints.state.ComplaintsListState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ComplaintsViewModel @Inject constructor(
    private val getMyComplaintsUseCase: GetMyComplaintsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ComplaintsListState())
    val state: StateFlow<ComplaintsListState> = _state.asStateFlow()

    init {
        loadComplaints()
    }

    fun onEvent(event: ComplaintsListEvent) {
        when (event) {
            is ComplaintsListEvent.LoadComplaints,
            is ComplaintsListEvent.Refresh -> loadComplaints()
        }
    }

    private fun loadComplaints() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = getMyComplaintsUseCase()
            result.fold(
                onSuccess = { list ->
                    _state.update { it.copy(isLoading = false, complaints = list, error = null) }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message ?: "Failed to load complaints") }
                }
            )
        }
    }
}
