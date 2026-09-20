package com.communityos.visitors.domain

import com.communityos.visitors.model.Visitor
import com.communityos.visitors.repository.VisitorRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMyVisitorsUseCase @Inject constructor(
    private val repository: VisitorRepository
) {
    suspend operator fun invoke(): Result<List<Visitor>> = repository.getMyVisitors()
    fun observe(): Flow<List<Visitor>> = repository.observeMyVisitors()
}
