package com.communityos.home.domain

import com.communityos.home.repository.HomeRepository
import javax.inject.Inject

class TriggerEmergencyAlertUseCase @Inject constructor(
    private val repository: HomeRepository
) {
    suspend operator fun invoke(type: String): Result<Unit> {
        return repository.triggerEmergencyAlert(type)
    }
}
