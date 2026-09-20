package com.communityos.security.state

import com.communityos.visitors.model.Visitor
import com.communityos.visitors.model.VisitorStatus

enum class SecurityTab {
    EXPECTED,
    CHECKED_IN,
    HISTORY
}

data class SecurityDashboardState(
    val isLoading: Boolean = false,
    val officerName: String = "",
    val communityName: String = "",
    val selectedTab: SecurityTab = SecurityTab.EXPECTED,
    val allVisitors: List<Visitor> = emptyList(),
    val error: String? = null
) {
    val expectedVisitors: List<Visitor>
        get() = allVisitors.filter { it.status == VisitorStatus.PRE_APPROVED }

    val checkedInVisitors: List<Visitor>
        get() = allVisitors.filter { it.status == VisitorStatus.CHECKED_IN }

    val historyVisitors: List<Visitor>
        get() = allVisitors.filter {
            it.status == VisitorStatus.CHECKED_OUT ||
            it.status == VisitorStatus.DENIED ||
            it.status == VisitorStatus.CANCELLED
        }

    val displayedVisitors: List<Visitor>
        get() = when (selectedTab) {
            SecurityTab.EXPECTED -> expectedVisitors
            SecurityTab.CHECKED_IN -> checkedInVisitors
            SecurityTab.HISTORY -> historyVisitors
        }
}
