package com.communityos.visitors.state

import com.communityos.visitors.model.Visitor
import com.communityos.visitors.model.VisitorStatus

enum class VisitorFilter {
    ALL,
    ACTIVE,
    PAST
}

data class ResidentVisitorsState(
    val isLoading: Boolean = false,
    val visitors: List<Visitor> = emptyList(),
    val filter: VisitorFilter = VisitorFilter.ALL,
    val error: String? = null
) {
    val filteredVisitors: List<Visitor>
        get() = when (filter) {
            VisitorFilter.ALL -> visitors
            VisitorFilter.ACTIVE -> visitors.filter {
                it.status == VisitorStatus.PRE_APPROVED || it.status == VisitorStatus.CHECKED_IN
            }
            VisitorFilter.PAST -> visitors.filter {
                it.status == VisitorStatus.CHECKED_OUT ||
                it.status == VisitorStatus.DENIED ||
                it.status == VisitorStatus.CANCELLED
            }
        }
}
