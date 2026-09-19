package com.communityos.complaints.domain

import com.communityos.complaints.model.Complaint
import com.communityos.complaints.repository.ComplaintRepository
import javax.inject.Inject

class CreateComplaintUseCase @Inject constructor(
    private val repository: ComplaintRepository
) {
    suspend operator fun invoke(category: String, description: String): Result<Complaint> {
        return repository.createComplaint(category, description)
    }
}
