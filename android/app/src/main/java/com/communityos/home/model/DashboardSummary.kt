package com.communityos.home.model

data class DashboardSummary(
    val activeVisitors: Int,
    val pendingComplaints: Int,
    val outstandingDues: Double,
    val communityName: String,
    val blockNo: String,
    val flatNo: String
)
