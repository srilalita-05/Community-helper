package com.communityos.notices.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.communityos.notices.domain.GetNoticeDetailsUseCase
import com.communityos.notices.event.NoticeDetailEvent
import com.communityos.notices.state.NoticeDetailState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoticeDetailViewModel @Inject constructor(
    private val getNoticeDetailsUseCase: GetNoticeDetailsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(NoticeDetailState())
    val state: StateFlow<NoticeDetailState> = _state.asStateFlow()

    init {
        val navNoticeId = savedStateHandle.get<String>("noticeId")
        if (!navNoticeId.isNullOrBlank()) {
            loadNotice(navNoticeId)
        }
    }

    fun onEvent(event: NoticeDetailEvent) {
        when (event) {
            is NoticeDetailEvent.LoadNotice -> loadNotice(event.noticeId)
            NoticeDetailEvent.DismissError -> _state.update { it.copy(errorMessage = null) }
        }
    }

    fun loadNotice(noticeId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            getNoticeDetailsUseCase(noticeId)
                .onSuccess { notice ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            notice = notice,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load notice details"
                        )
                    }
                }
        }
    }
}
