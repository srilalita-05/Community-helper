package com.communityos.visitors.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.communityos.visitors.domain.GetMyVisitorsUseCase
import com.communityos.visitors.event.ResidentVisitorsEvent
import com.communityos.visitors.state.ResidentVisitorsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResidentVisitorsViewModel @Inject constructor(
    private val getMyVisitorsUseCase: GetMyVisitorsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ResidentVisitorsState())
    val state: StateFlow<ResidentVisitorsState> = _state.asStateFlow()

    init {
        loadVisitors()
    }

    fun onEvent(event: ResidentVisitorsEvent) {
        when (event) {
            is ResidentVisitorsEvent.LoadVisitors,
            is ResidentVisitorsEvent.Refresh -> loadVisitors()
            is ResidentVisitorsEvent.SetFilter -> {
                _state.update { it.copy(filter = event.filter) }
            }
        }
    }

    private fun loadVisitors() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = getMyVisitorsUseCase()
            result.fold(
                onSuccess = { list ->
                    _state.update { it.copy(isLoading = false, visitors = list, error = null) }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(isLoading = false, error = error.message ?: "Failed to load visitors")
                    }
                }
            )
        }
    }
}
