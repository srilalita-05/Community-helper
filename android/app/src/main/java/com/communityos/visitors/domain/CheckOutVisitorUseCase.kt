package com.communityos.visitors.domain

import com.communityos.visitors.model.Visitor
import com.communityos.visitors.repository.VisitorRepository
import javax.inject.Inject

class CheckOutVisitorUseCase @Inject constructor(
    private val repository: VisitorRepository
) {
    suspend operator fun invoke(visitorId: String): Result<Visitor> =
        repository.checkOutVisitor(visitorId)
}
