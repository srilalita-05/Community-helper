package com.communityos.notices.event

sealed interface NoticesListEvent {
    object LoadNotices : NoticesListEvent
    object Refresh : NoticesListEvent
    object DismissError : NoticesListEvent
}
