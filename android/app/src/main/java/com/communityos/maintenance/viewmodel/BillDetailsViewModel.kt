package com.communityos.maintenance.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.communityos.maintenance.domain.GetBillDetailsUseCase
import com.communityos.maintenance.event.BillDetailsEvent
import com.communityos.maintenance.state.BillDetailsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BillDetailsViewModel @Inject constructor(
    private val getBillDetailsUseCase: GetBillDetailsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val billId: String = checkNotNull(savedStateHandle["billId"])

    private val _state = MutableStateFlow(BillDetailsState())
    val state: StateFlow<BillDetailsState> = _state.asStateFlow()

    init {
        loadDetails()
    }

    fun onEvent(event: BillDetailsEvent) {
        when (event) {
            is BillDetailsEvent.Refresh -> loadDetails()
            is BillDetailsEvent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    fun loadDetails() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = getBillDetailsUseCase(billId)
            result.fold(
                onSuccess = { detail ->
                    _state.update { it.copy(isLoading = false, billDetail = detail, error = null) }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.localizedMessage ?: "Failed to load bill details"
                        )
                    }
                }
            )
        }
    }
}
