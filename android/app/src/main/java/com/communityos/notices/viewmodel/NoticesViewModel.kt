package com.communityos.notices.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.communityos.notices.domain.GetCommunityNoticesUseCase
import com.communityos.notices.event.NoticesListEvent
import com.communityos.notices.state.NoticesListState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoticesViewModel @Inject constructor(
    private val getCommunityNoticesUseCase: GetCommunityNoticesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(NoticesListState())
    val state: StateFlow<NoticesListState> = _state.asStateFlow()

    init {
        onEvent(NoticesListEvent.LoadNotices)
    }

    fun onEvent(event: NoticesListEvent) {
        when (event) {
            NoticesListEvent.LoadNotices, NoticesListEvent.Refresh -> loadNotices()
            NoticesListEvent.DismissError -> {
                _state.update { it.copy(errorMessage = null) }
            }
        }
    }

    private fun loadNotices() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            getCommunityNoticesUseCase()
                .onSuccess { notices ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            notices = notices,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load notices"
                        )
                    }
                }
        }
    }
}
