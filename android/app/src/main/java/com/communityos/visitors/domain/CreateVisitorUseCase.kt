package com.communityos.visitors.domain

import com.communityos.visitors.model.Visitor
import com.communityos.visitors.repository.VisitorRepository
import javax.inject.Inject

class CreateVisitorUseCase @Inject constructor(
    private val repository: VisitorRepository
) {
    suspend operator fun invoke(
        name: String,
        phoneNumber: String,
        purpose: String,
        scheduledArrivalDate: Long,
        vehicleNumber: String? = null
    ): Result<Visitor> = repository.createVisitor(
        name = name,
        phoneNumber = phoneNumber,
        purpose = purpose,
        scheduledArrivalDate = scheduledArrivalDate,
        vehicleNumber = vehicleNumber
    )
}
