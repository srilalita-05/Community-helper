package com.communityos.security.state

import com.communityos.visitors.model.SecurityVisitorDetail

data class SecurityVisitorDetailState(
    val isLoading: Boolean = false,
    val detail: SecurityVisitorDetail? = null,
    val isProcessing: Boolean = false,
    val actionSuccessMessage: String? = null,
    val error: String? = null
)
