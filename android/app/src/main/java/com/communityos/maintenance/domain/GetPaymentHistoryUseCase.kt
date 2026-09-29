package com.communityos.maintenance.domain

import com.communityos.maintenance.model.MaintenancePayment
import com.communityos.maintenance.repository.MaintenanceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPaymentHistoryUseCase @Inject constructor(
    private val repository: MaintenanceRepository
) {
    suspend operator fun invoke(): Result<List<MaintenancePayment>> {
        return repository.getPaymentHistory()
    }

    fun observe(): Flow<List<MaintenancePayment>> {
        return repository.observePaymentHistory()
    }
}
