package com.communityos.marketplace.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.communityos.marketplace.domain.DeleteListingUseCase
import com.communityos.marketplace.domain.GetListingDetailsUseCase
import com.communityos.marketplace.domain.UpdateListingUseCase
import com.communityos.marketplace.event.ListingDetailEvent
import com.communityos.marketplace.state.ListingDetailState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListingDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getListingDetailsUseCase: GetListingDetailsUseCase,
    private val updateListingUseCase: UpdateListingUseCase,
    private val deleteListingUseCase: DeleteListingUseCase
) : ViewModel() {

    val listingId: String = checkNotNull(savedStateHandle["listingId"])

    private val _state = MutableStateFlow(ListingDetailState())
    val state: StateFlow<ListingDetailState> = _state.asStateFlow()

    init {
        loadDetails()
    }

    fun onEvent(event: ListingDetailEvent) {
        when (event) {
            is ListingDetailEvent.Refresh -> loadDetails()
            is ListingDetailEvent.ShowDeleteDialog -> _state.update { it.copy(showDeleteDialog = true) }
            is ListingDetailEvent.DismissDeleteDialog -> _state.update { it.copy(showDeleteDialog = false) }
            is ListingDetailEvent.ConfirmDelete -> deleteListing()
            is ListingDetailEvent.ShowSoldDialog -> _state.update { it.copy(showSoldDialog = true) }
            is ListingDetailEvent.DismissSoldDialog -> _state.update { it.copy(showSoldDialog = false) }
            is ListingDetailEvent.ConfirmMarkSold -> markAsSold()
            is ListingDetailEvent.ShowCancelDialog -> _state.update { it.copy(showCancelDialog = true) }
            is ListingDetailEvent.DismissCancelDialog -> _state.update { it.copy(showCancelDialog = false) }
            is ListingDetailEvent.ConfirmCancelListing -> cancelListing()
            is ListingDetailEvent.DismissMessage -> _state.update { it.copy(actionMessage = null, errorMessage = null) }
        }
    }

    private fun loadDetails() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            getListingDetailsUseCase(listingId).fold(
                onSuccess = { detail ->
                    _state.update { it.copy(isLoading = false, detail = detail, errorMessage = null) }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.message ?: "Failed to load listing") }
                }
            )
        }
    }

    private fun markAsSold() {
        viewModelScope.launch {
            _state.update { it.copy(showSoldDialog = false, isLoading = true) }
            updateListingUseCase.markAsSold(listingId).fold(
                onSuccess = {
                    loadDetails()
                    _state.update { it.copy(actionMessage = "Listing marked as Sold") }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            )
        }
    }

    private fun cancelListing() {
        viewModelScope.launch {
            _state.update { it.copy(showCancelDialog = false, isLoading = true) }
            updateListingUseCase.cancel(listingId).fold(
                onSuccess = {
                    loadDetails()
                    _state.update { it.copy(actionMessage = "Listing cancelled") }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            )
        }
    }

    private fun deleteListing() {
        viewModelScope.launch {
            _state.update { it.copy(showDeleteDialog = false, isLoading = true) }
            deleteListingUseCase(listingId).fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false, isDeleted = true) }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            )
        }
    }
}
