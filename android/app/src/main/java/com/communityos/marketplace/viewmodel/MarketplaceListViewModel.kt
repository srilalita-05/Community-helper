package com.communityos.marketplace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.communityos.marketplace.domain.GetCommunityListingsUseCase
import com.communityos.marketplace.domain.GetMyListingsUseCase
import com.communityos.marketplace.event.MarketplaceListEvent
import com.communityos.marketplace.state.MarketplaceListState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MarketplaceListViewModel @Inject constructor(
    private val getCommunityListingsUseCase: GetCommunityListingsUseCase,
    private val getMyListingsUseCase: GetMyListingsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MarketplaceListState())
    val state: StateFlow<MarketplaceListState> = _state.asStateFlow()

    init {
        loadListings()
        observeListings()
    }

    fun onEvent(event: MarketplaceListEvent) {
        when (event) {
            is MarketplaceListEvent.Refresh -> loadListings()
            is MarketplaceListEvent.SelectTab -> {
                _state.update { it.copy(selectedTab = event.tab) }
            }
            is MarketplaceListEvent.SelectCategory -> {
                _state.update { it.copy(selectedCategory = event.category) }
            }
        }
    }

    private fun loadListings() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val communityRes = getCommunityListingsUseCase()
            val myRes = getMyListingsUseCase()

            if (communityRes.isFailure && myRes.isFailure) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = communityRes.exceptionOrNull()?.message ?: "Failed to load listings"
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        isLoading = false,
                        communityListings = communityRes.getOrDefault(emptyList()),
                        myListings = myRes.getOrDefault(emptyList()),
                        errorMessage = null
                    )
                }
            }
        }
    }

    private fun observeListings() {
        viewModelScope.launch {
            getCommunityListingsUseCase.observe().collect { listings ->
                _state.update { it.copy(communityListings = listings) }
            }
        }
        viewModelScope.launch {
            getMyListingsUseCase.observe().collect { listings ->
                _state.update { it.copy(myListings = listings) }
            }
        }
    }
}
