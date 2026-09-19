package com.communityos.notices.state

import com.communityos.notices.model.Notice

data class NoticeDetailState(
    val isLoading: Boolean = false,
    val notice: Notice? = null,
    val errorMessage: String? = null
)
