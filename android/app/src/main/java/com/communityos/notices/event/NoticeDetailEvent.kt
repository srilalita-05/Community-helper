package com.communityos.notices.event

sealed interface NoticeDetailEvent {
    data class LoadNotice(val noticeId: String) : NoticeDetailEvent
    object DismissError : NoticeDetailEvent
}
