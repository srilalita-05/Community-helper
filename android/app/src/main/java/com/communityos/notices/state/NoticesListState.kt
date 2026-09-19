package com.communityos.notices.state

import com.communityos.notices.model.Notice

data class NoticesListState(
    val isLoading: Boolean = false,
    val notices: List<Notice> = emptyList(),
    val errorMessage: String? = null
)
