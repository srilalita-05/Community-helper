package com.communityos.complaints

import androidx.lifecycle.SavedStateHandle
import com.communityos.complaints.domain.GetComplaintDetailsUseCase
import com.communityos.complaints.event.ComplaintDetailEvent
import com.communityos.complaints.model.Complaint
import com.communityos.complaints.model.ComplaintStatus
import com.communityos.complaints.repository.ComplaintRepository
import com.communityos.complaints.viewmodel.ComplaintDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ComplaintDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeComplaintRepository
    private lateinit var getComplaintDetailsUseCase: GetComplaintDetailsUseCase
    private lateinit var viewModel: ComplaintDetailViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeComplaintRepository()
        getComplaintDetailsUseCase = GetComplaintDetailsUseCase(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsComplaintDetailsSuccessfully() = runTest(testDispatcher) {
        val sample = Complaint(
            id = "complaint_100",
            residentId = "res_1",
            communityId = "comm_1",
            flatId = "flat_1",
            category = "Water",
            description = "Pipe burst in balcony",
            status = ComplaintStatus.SUBMITTED,
            createdAt = 5000L
        )
        fakeRepository.detailResult = Result.success(sample)

        val handle = SavedStateHandle(mapOf("complaintId" to "complaint_100"))
        viewModel = ComplaintDetailViewModel(handle, getComplaintDetailsUseCase)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("complaint_100", state.complaint?.id)
        assertEquals("Pipe burst in balcony", state.complaint?.description)
    }

    @Test
    fun initialState_handlesError_whenComplaintNotFoundOrAccessDenied() = runTest(testDispatcher) {
        fakeRepository.detailResult = Result.failure(NoSuchElementException("Complaint not found for active resident"))

        val handle = SavedStateHandle(mapOf("complaintId" to "complaint_other"))
        viewModel = ComplaintDetailViewModel(handle, getComplaintDetailsUseCase)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.complaint)
        assertEquals("Complaint not found for active resident", state.error)
    }

    @Test
    fun refreshEvent_reloadsDetail() = runTest(testDispatcher) {
        fakeRepository.detailResult = Result.failure(RuntimeException("Network error"))

        val handle = SavedStateHandle(mapOf("complaintId" to "complaint_100"))
        viewModel = ComplaintDetailViewModel(handle, getComplaintDetailsUseCase)
        testScheduler.advanceUntilIdle()

        assertNotNull(viewModel.state.value.error)

        fakeRepository.detailResult = Result.success(
            Complaint(
                id = "complaint_100",
                residentId = "res_1",
                communityId = "comm_1",
                flatId = "flat_1",
                category = "Water",
                description = "Fixed pipe",
                status = ComplaintStatus.RESOLVED,
                createdAt = 5000L
            )
        )

        viewModel.onEvent(ComplaintDetailEvent.Refresh)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertNull(state.error)
        assertEquals(ComplaintStatus.RESOLVED, state.complaint?.status)
    }

    private class FakeComplaintRepository : ComplaintRepository {
        var detailResult: Result<Complaint> = Result.failure(NotImplementedError())

        override suspend fun getMyComplaints(): Result<List<Complaint>> = Result.success(emptyList())

        override suspend fun getComplaintDetails(complaintId: String): Result<Complaint> =
            detailResult

        override suspend fun createComplaint(category: String, description: String): Result<Complaint> =
            Result.failure(NotImplementedError())

        override suspend fun getPendingComplaintsCount(): Result<Int> = Result.success(0)
    }
}
