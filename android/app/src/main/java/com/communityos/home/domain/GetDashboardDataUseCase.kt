package com.communityos.home.domain

import com.communityos.home.model.DashboardSummary
import com.communityos.home.repository.HomeRepository
import javax.inject.Inject

class GetDashboardDataUseCase @Inject constructor(
    private val repository: HomeRepository
) {
    suspend operator fun invoke(): Result<DashboardSummary> {
        return repository.getDashboardSummary()
    }
}
