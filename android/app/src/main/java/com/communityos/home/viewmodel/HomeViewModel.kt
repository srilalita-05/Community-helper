package com.communityos.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.communityos.authentication.domain.LogoutUseCase
import com.communityos.home.domain.GetDashboardDataUseCase
import com.communityos.home.domain.TriggerEmergencyAlertUseCase
import com.communityos.home.event.HomeEffect
import com.communityos.home.event.HomeEvent
import com.communityos.home.state.HomeState
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
class HomeViewModel @Inject constructor(
    private val getDashboardDataUseCase: GetDashboardDataUseCase,
    private val triggerEmergencyAlertUseCase: TriggerEmergencyAlertUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect: Flow<HomeEffect> = _effect.receiveAsFlow()

    init {
        onEvent(HomeEvent.LoadSummary)
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.LoadSummary -> loadSummary()
            is HomeEvent.TriggerEmergency -> triggerEmergency(event.type)
            HomeEvent.DismissEmergency -> {
                _state.update { it.copy(isEmergencyTriggered = false, emergencyType = null) }
            }
            HomeEvent.ClearError -> {
                _state.update { it.copy(errorMessage = null) }
            }
            HomeEvent.Logout -> logout()
        }
    }

    private fun loadSummary() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = getDashboardDataUseCase()
            result.fold(
                onSuccess = { summary ->
                    _state.update { it.copy(isLoading = false, summary = summary) }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            )
        }
    }

    private fun triggerEmergency(type: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = triggerEmergencyAlertUseCase(type)
            result.fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isEmergencyTriggered = true,
                            emergencyType = type
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            )
        }
    }

    private fun logout() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = logoutUseCase()
            result.fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                    _effect.send(HomeEffect.NavigateToOnboarding)
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            )
        }
    }
}
