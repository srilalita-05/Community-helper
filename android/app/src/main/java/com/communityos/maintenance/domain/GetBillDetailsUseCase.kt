package com.communityos.maintenance.domain

import com.communityos.maintenance.model.MaintenanceBillDetail
import com.communityos.maintenance.repository.MaintenanceRepository
import javax.inject.Inject

class GetBillDetailsUseCase @Inject constructor(
    private val repository: MaintenanceRepository
) {
    suspend operator fun invoke(billId: String): Result<MaintenanceBillDetail> {
        return repository.getBillDetails(billId)
    }
}
