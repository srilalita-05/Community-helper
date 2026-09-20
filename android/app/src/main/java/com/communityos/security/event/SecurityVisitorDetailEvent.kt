package com.communityos.security.event

sealed class SecurityVisitorDetailEvent {
    object LoadDetails : SecurityVisitorDetailEvent()
    object CheckIn : SecurityVisitorDetailEvent()
    object CheckOut : SecurityVisitorDetailEvent()
    object Deny : SecurityVisitorDetailEvent()
    object ClearSuccess : SecurityVisitorDetailEvent()
}
