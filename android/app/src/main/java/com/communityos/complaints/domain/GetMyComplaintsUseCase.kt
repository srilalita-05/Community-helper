package com.communityos.complaints.domain

import com.communityos.complaints.model.Complaint
import com.communityos.complaints.repository.ComplaintRepository
import javax.inject.Inject

class GetMyComplaintsUseCase @Inject constructor(
    private val repository: ComplaintRepository
) {
    suspend operator fun invoke(): Result<List<Complaint>> {
        return repository.getMyComplaints()
    }
}
