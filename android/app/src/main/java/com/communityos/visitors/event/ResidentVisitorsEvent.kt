package com.communityos.visitors.event

import com.communityos.visitors.state.VisitorFilter

sealed class ResidentVisitorsEvent {
    object LoadVisitors : ResidentVisitorsEvent()
    object Refresh : ResidentVisitorsEvent()
    data class SetFilter(val filter: VisitorFilter) : ResidentVisitorsEvent()
}
