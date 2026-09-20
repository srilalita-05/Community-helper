package com.communityos.visitors.event

sealed class CreateVisitorEvent {
    data class OnNameChanged(val name: String) : CreateVisitorEvent()
    data class OnPhoneChanged(val phone: String) : CreateVisitorEvent()
    data class OnPurposeChanged(val purpose: String) : CreateVisitorEvent()
    data class OnVehicleChanged(val vehicle: String) : CreateVisitorEvent()
    data class OnDateChanged(val date: Long) : CreateVisitorEvent()
    object Submit : CreateVisitorEvent()
    object ResetSuccess : CreateVisitorEvent()
}
