package com.communityos.authentication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.communityos.authentication.domain.RestoreSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StartupDestination {
    HOME,
    AUTH
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val restoreSessionUseCase: RestoreSessionUseCase
) : ViewModel() {

    private val _destination = MutableStateFlow<StartupDestination?>(null)
    val destination: StateFlow<StartupDestination?> = _destination.asStateFlow()

    init {
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            val result = restoreSessionUseCase()
            val user = result.getOrNull()
            if (user != null) {
                _destination.value = StartupDestination.HOME
            } else {
                _destination.value = StartupDestination.AUTH
            }
        }
    }
}
