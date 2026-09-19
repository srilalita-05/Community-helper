package com.communityos.complaints.event

sealed class ComplaintsListEvent {
    object LoadComplaints : ComplaintsListEvent()
    object Refresh : ComplaintsListEvent()
}
