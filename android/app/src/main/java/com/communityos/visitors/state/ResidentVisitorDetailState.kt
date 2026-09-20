package com.communityos.visitors.state

import com.communityos.visitors.model.Visitor

data class ResidentVisitorDetailState(
    val isLoading: Boolean = false,
    val visitor: Visitor? = null,
    val isCancelling: Boolean = false,
    val cancelSuccess: Boolean = false,
    val error: String? = null
)
