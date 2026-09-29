package com.communityos.maintenance.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.communityos.maintenance.domain.GetBillDetailsUseCase
import com.communityos.maintenance.domain.PayMaintenanceBillUseCase
import com.communityos.maintenance.event.PaymentConfirmationEvent
import com.communityos.maintenance.event.PaymentEffect
import com.communityos.maintenance.state.PaymentConfirmationState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentConfirmationViewModel @Inject constructor(
    private val getBillDetailsUseCase: GetBillDetailsUseCase,
    private val payMaintenanceBillUseCase: PayMaintenanceBillUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val billId: String = checkNotNull(savedStateHandle["billId"])

    private val _state = MutableStateFlow(PaymentConfirmationState())
    val state: StateFlow<PaymentConfirmationState> = _state.asStateFlow()

    private val _effect = Channel<PaymentEffect>(Channel.BUFFERED)
    val effect: Flow<PaymentEffect> = _effect.receiveAsFlow()

    init {
        loadBill()
    }

    fun onEvent(event: PaymentConfirmationEvent) {
        when (event) {
            is PaymentConfirmationEvent.Refresh -> loadBill()
            is PaymentConfirmationEvent.SelectMethod -> {
                _state.update { it.copy(selectedMethod = event.method) }
            }
            is PaymentConfirmationEvent.ConfirmPayment -> processPayment()
            is PaymentConfirmationEvent.DismissError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    fun loadBill() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = getBillDetailsUseCase(billId)
            result.fold(
                onSuccess = { detail ->
                    _state.update { it.copy(isLoading = false, bill = detail.bill, error = null) }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.localizedMessage ?: "Failed to load bill"
                        )
                    }
                }
            )
        }
    }

    private fun processPayment() {
        if (_state.value.isProcessing || _state.value.isSuccess) return

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, error = null) }
            val method = _state.value.selectedMethod
            val result = payMaintenanceBillUseCase(billId, method)
            result.fold(
                onSuccess = { receipt ->
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            isSuccess = true,
                            receipt = receipt,
                            bill = receipt.bill,
                            error = null
                        )
                    }
                    _effect.send(PaymentEffect.PaymentSuccess(receipt.payment.transactionRef))
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = error.localizedMessage ?: "Payment failed. Please try again."
                        )
                    }
                }
            )
        }
    }
}
