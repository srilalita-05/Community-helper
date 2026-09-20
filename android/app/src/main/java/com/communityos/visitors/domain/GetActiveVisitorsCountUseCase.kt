package com.communityos.visitors.domain

import com.communityos.visitors.repository.VisitorRepository
import javax.inject.Inject

class GetActiveVisitorsCountUseCase @Inject constructor(
    private val repository: VisitorRepository
) {
    suspend operator fun invoke(): Result<Int> = repository.getActiveVisitorsCount()
}
