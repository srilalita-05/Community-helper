package com.communityos.maintenance.state

import com.communityos.maintenance.model.MaintenanceBillDetail

data class BillDetailsState(
    val isLoading: Boolean = true,
    val billDetail: MaintenanceBillDetail? = null,
    val error: String? = null
)
