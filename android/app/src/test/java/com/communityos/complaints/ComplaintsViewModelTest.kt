package com.communityos.complaints

import com.communityos.complaints.domain.GetMyComplaintsUseCase
import com.communityos.complaints.event.ComplaintsListEvent
import com.communityos.complaints.model.Complaint
import com.communityos.complaints.model.ComplaintStatus
import com.communityos.complaints.repository.ComplaintRepository
import com.communityos.complaints.viewmodel.ComplaintsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ComplaintsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeComplaintRepository
    private lateinit var getMyComplaintsUseCase: GetMyComplaintsUseCase
    private lateinit var viewModel: ComplaintsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeComplaintRepository()
        getMyComplaintsUseCase = GetMyComplaintsUseCase(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsComplaintsSuccessfully() = runTest(testDispatcher) {
        val sampleComplaints = listOf(
            Complaint(
                id = "c_1",
                residentId = "res_1",
                communityId = "comm_1",
                flatId = "flat_1",
                category = "Water",
                description = "Water leakage",
                status = ComplaintStatus.SUBMITTED,
                createdAt = 1000L
            )
        )
        fakeRepository.complaintsResult = Result.success(sampleComplaints)

        viewModel = ComplaintsViewModel(getMyComplaintsUseCase)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(1, state.complaints.size)
        assertEquals("c_1", state.complaints[0].id)
    }

    @Test
    fun initialState_handlesEmptyState() = runTest(testDispatcher) {
        fakeRepository.complaintsResult = Result.success(emptyList())

        viewModel = ComplaintsViewModel(getMyComplaintsUseCase)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertTrue(state.complaints.isEmpty())
    }

    @Test
    fun initialState_handlesError() = runTest(testDispatcher) {
        fakeRepository.complaintsResult = Result.failure(RuntimeException("Database connection failed"))

        viewModel = ComplaintsViewModel(getMyComplaintsUseCase)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("Database connection failed", state.error)
        assertTrue(state.complaints.isEmpty())
    }

    @Test
    fun refreshEvent_reloadsComplaints() = runTest(testDispatcher) {
        fakeRepository.complaintsResult = Result.success(emptyList())
        viewModel = ComplaintsViewModel(getMyComplaintsUseCase)
        testScheduler.advanceUntilIdle()

        assertEquals(0, viewModel.state.value.complaints.size)

        fakeRepository.complaintsResult = Result.success(
            listOf(
                Complaint(
                    id = "c_2",
                    residentId = "res_1",
                    communityId = "comm_1",
                    flatId = "flat_1",
                    category = "Electricity",
                    description = "Flickering lights",
                    status = ComplaintStatus.IN_PROGRESS,
                    createdAt = 2000L
                )
            )
        )

        viewModel.onEvent(ComplaintsListEvent.Refresh)
        testScheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.complaints.size)
        assertEquals("c_2", viewModel.state.value.complaints[0].id)
    }

    private class FakeComplaintRepository : ComplaintRepository {
        var complaintsResult: Result<List<Complaint>> = Result.success(emptyList())

        override suspend fun getMyComplaints(): Result<List<Complaint>> = complaintsResult

        override suspend fun getComplaintDetails(complaintId: String): Result<Complaint> =
            Result.failure(NotImplementedError())

        override suspend fun createComplaint(category: String, description: String): Result<Complaint> =
            Result.failure(NotImplementedError())

        override suspend fun getPendingComplaintsCount(): Result<Int> = Result.success(0)
    }
}
