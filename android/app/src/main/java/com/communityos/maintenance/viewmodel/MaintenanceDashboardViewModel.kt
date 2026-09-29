package com.communityos.maintenance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.communityos.maintenance.domain.GetBillHistoryUseCase
import com.communityos.maintenance.domain.GetPaymentHistoryUseCase
import com.communityos.maintenance.domain.GetPendingDuesUseCase
import com.communityos.maintenance.event.MaintenanceDashboardEvent
import com.communityos.maintenance.state.MaintenanceDashboardState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MaintenanceDashboardViewModel @Inject constructor(
    private val getPendingDuesUseCase: GetPendingDuesUseCase,
    private val getBillHistoryUseCase: GetBillHistoryUseCase,
    private val getPaymentHistoryUseCase: GetPaymentHistoryUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MaintenanceDashboardState())
    val state: StateFlow<MaintenanceDashboardState> = _state.asStateFlow()

    init {
        observeData()
    }

    fun onEvent(event: MaintenanceDashboardEvent) {
        when (event) {
            is MaintenanceDashboardEvent.Refresh -> loadData()
            is MaintenanceDashboardEvent.SelectTab -> {
                _state.update { it.copy(selectedTab = event.tab) }
            }
            is MaintenanceDashboardEvent.DismissError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                getPendingDuesUseCase.observeOutstandingTotal(),
                getPendingDuesUseCase.observePendingBills(),
                getBillHistoryUseCase.observe(),
                getPaymentHistoryUseCase.observe()
            ) { total, pending, history, payments ->
                MaintenanceDashboardState(
                    isLoading = false,
                    outstandingTotal = total,
                    pendingBills = pending,
                    billHistory = history,
                    payments = payments,
                    selectedTab = _state.value.selectedTab,
                    error = null
                )
            }.catch { e ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "Failed to observe maintenance data"
                    )
                }
            }.collect { newState ->
                _state.value = newState
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val totalResult = getPendingDuesUseCase.getOutstandingTotal()
                val pendingResult = getPendingDuesUseCase.getPendingBills()
                val historyResult = getBillHistoryUseCase()
                val paymentsResult = getPaymentHistoryUseCase()

                if (totalResult.isSuccess && pendingResult.isSuccess && historyResult.isSuccess && paymentsResult.isSuccess) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            outstandingTotal = totalResult.getOrDefault(0.0),
                            pendingBills = pendingResult.getOrDefault(emptyList()),
                            billHistory = historyResult.getOrDefault(emptyList()),
                            payments = paymentsResult.getOrDefault(emptyList()),
                            error = null
                        )
                    }
                } else {
                    val errorMsg = totalResult.exceptionOrNull()?.localizedMessage
                        ?: pendingResult.exceptionOrNull()?.localizedMessage
                        ?: historyResult.exceptionOrNull()?.localizedMessage
                        ?: paymentsResult.exceptionOrNull()?.localizedMessage
                        ?: "Failed to load maintenance records"
                    _state.update { it.copy(isLoading = false, error = errorMsg) }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.localizedMessage ?: "Unexpected error")
                }
            }
        }
    }
}
