package com.communityos.visitors.event

sealed class ResidentVisitorDetailEvent {
    object LoadDetails : ResidentVisitorDetailEvent()
    object CancelPass : ResidentVisitorDetailEvent()
    object ResetCancelSuccess : ResidentVisitorDetailEvent()
}
