package com.communityos.security.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.communityos.authentication.domain.LogoutUseCase
import com.communityos.security.event.SecurityDashboardEffect
import com.communityos.security.event.SecurityDashboardEvent
import com.communityos.security.state.SecurityDashboardState
import com.communityos.visitors.domain.GetSecurityGateProfileUseCase
import com.communityos.visitors.domain.GetSecurityVisitorsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityDashboardViewModel @Inject constructor(
    private val getSecurityVisitorsUseCase: GetSecurityVisitorsUseCase,
    private val getSecurityGateProfileUseCase: GetSecurityGateProfileUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SecurityDashboardState())
    val state: StateFlow<SecurityDashboardState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<SecurityDashboardEffect>()
    val effect: SharedFlow<SecurityDashboardEffect> = _effect.asSharedFlow()

    init {
        loadOfficerProfile()
        loadVisitors()
    }

    fun onEvent(event: SecurityDashboardEvent) {
        when (event) {
            is SecurityDashboardEvent.LoadDashboard,
            is SecurityDashboardEvent.Refresh -> {
                loadOfficerProfile()
                loadVisitors()
            }
            is SecurityDashboardEvent.SelectTab -> {
                _state.update { it.copy(selectedTab = event.tab) }
            }
            is SecurityDashboardEvent.Logout -> logout()
        }
    }

    private fun loadOfficerProfile() {
        viewModelScope.launch {
            val result = getSecurityGateProfileUseCase()
            result.onSuccess { profile ->
                _state.update {
                    it.copy(
                        officerName = profile.officerName,
                        communityName = profile.communityName
                    )
                }
            }
        }
    }

    private fun loadVisitors() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = getSecurityVisitorsUseCase()
            result.fold(
                onSuccess = { list ->
                    _state.update { it.copy(isLoading = false, allVisitors = list, error = null) }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(isLoading = false, error = error.message ?: "Failed to load visitors")
                    }
                }
            )
        }
    }

    private fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _effect.emit(SecurityDashboardEffect.NavigateToOnboarding)
        }
    }
}
