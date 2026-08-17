package com.communityos.home.event

sealed class HomeEvent {
    object LoadSummary : HomeEvent()
    data class TriggerEmergency(val type: String) : HomeEvent()
    object DismissEmergency : HomeEvent()
    object ClearError : HomeEvent()
}
