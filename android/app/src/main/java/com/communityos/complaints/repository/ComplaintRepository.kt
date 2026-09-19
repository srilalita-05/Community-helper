package com.communityos.complaints.repository

import com.communityos.complaints.model.Complaint

interface ComplaintRepository {
    suspend fun getMyComplaints(): Result<List<Complaint>>
    suspend fun getComplaintDetails(complaintId: String): Result<Complaint>
    suspend fun createComplaint(category: String, description: String): Result<Complaint>
    suspend fun getPendingComplaintsCount(): Result<Int>
}
