package com.communityos.maintenance.domain

import com.communityos.maintenance.model.MaintenanceBill
import com.communityos.maintenance.repository.MaintenanceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPendingDuesUseCase @Inject constructor(
    private val repository: MaintenanceRepository
) {
    suspend fun getOutstandingTotal(): Result<Double> {
        return repository.getOutstandingDues()
    }

    fun observeOutstandingTotal(): Flow<Double> {
        return repository.observeOutstandingDues()
    }

    suspend fun getPendingBills(): Result<List<MaintenanceBill>> {
        return repository.getPendingBills()
    }

    fun observePendingBills(): Flow<List<MaintenanceBill>> {
        return repository.observePendingBills()
    }
}
