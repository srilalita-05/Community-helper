package com.communityos.complaints.event

sealed class ComplaintDetailEvent {
    object LoadDetail : ComplaintDetailEvent()
    object Refresh : ComplaintDetailEvent()
}
