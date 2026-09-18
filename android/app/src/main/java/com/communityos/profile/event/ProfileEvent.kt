package com.communityos.profile.event

sealed class ProfileEvent {
    object LoadProfile : ProfileEvent()
    object StartEditing : ProfileEvent()
    object CancelEditing : ProfileEvent()
    data class NameChanged(val name: String) : ProfileEvent()
    data class EmailChanged(val email: String) : ProfileEvent()
    object SaveProfile : ProfileEvent()
    object ClearMessages : ProfileEvent()
}
