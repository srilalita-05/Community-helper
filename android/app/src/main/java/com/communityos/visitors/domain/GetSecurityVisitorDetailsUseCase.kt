package com.communityos.visitors.domain

import com.communityos.visitors.model.SecurityVisitorDetail
import com.communityos.visitors.repository.VisitorRepository
import javax.inject.Inject

class GetSecurityVisitorDetailsUseCase @Inject constructor(
    private val repository: VisitorRepository
) {
    suspend operator fun invoke(visitorId: String): Result<SecurityVisitorDetail> =
        repository.getSecurityVisitorDetails(visitorId)
}
