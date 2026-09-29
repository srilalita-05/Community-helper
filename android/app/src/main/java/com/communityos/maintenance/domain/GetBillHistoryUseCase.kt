package com.communityos.maintenance.domain

import com.communityos.maintenance.model.MaintenanceBill
import com.communityos.maintenance.repository.MaintenanceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBillHistoryUseCase @Inject constructor(
    private val repository: MaintenanceRepository
) {
    suspend operator fun invoke(): Result<List<MaintenanceBill>> {
        return repository.getBillHistory()
    }

    fun observe(): Flow<List<MaintenanceBill>> {
        return repository.observeBillHistory()
    }
}
