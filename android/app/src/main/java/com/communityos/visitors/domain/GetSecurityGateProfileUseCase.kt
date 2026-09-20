package com.communityos.visitors.domain

import com.communityos.visitors.model.SecurityGateProfile
import com.communityos.visitors.repository.VisitorRepository
import javax.inject.Inject

class GetSecurityGateProfileUseCase @Inject constructor(
    private val repository: VisitorRepository
) {
    suspend operator fun invoke(): Result<SecurityGateProfile> =
        repository.getSecurityGateProfile()
}
