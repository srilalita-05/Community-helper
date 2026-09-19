package com.communityos.complaints.domain

import com.communityos.complaints.model.Complaint
import com.communityos.complaints.repository.ComplaintRepository
import javax.inject.Inject

class GetComplaintDetailsUseCase @Inject constructor(
    private val repository: ComplaintRepository
) {
    suspend operator fun invoke(complaintId: String): Result<Complaint> {
        return repository.getComplaintDetails(complaintId)
    }
}
