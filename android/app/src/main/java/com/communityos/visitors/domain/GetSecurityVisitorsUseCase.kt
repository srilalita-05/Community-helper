package com.communityos.visitors.domain

import com.communityos.visitors.model.Visitor
import com.communityos.visitors.repository.VisitorRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSecurityVisitorsUseCase @Inject constructor(
    private val repository: VisitorRepository
) {
    suspend operator fun invoke(): Result<List<Visitor>> = repository.getSecurityVisitors()
    fun observe(): Flow<List<Visitor>> = repository.observeSecurityVisitors()
}
