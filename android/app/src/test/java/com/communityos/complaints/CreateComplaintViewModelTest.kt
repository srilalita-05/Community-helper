package com.communityos.complaints

import com.communityos.complaints.domain.CreateComplaintUseCase
import com.communityos.complaints.event.CreateComplaintEvent
import com.communityos.complaints.model.Complaint
import com.communityos.complaints.model.ComplaintStatus
import com.communityos.complaints.repository.ComplaintRepository
import com.communityos.complaints.viewmodel.CreateComplaintViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateComplaintViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeComplaintRepository
    private lateinit var createComplaintUseCase: CreateComplaintUseCase
    private lateinit var viewModel: CreateComplaintViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeComplaintRepository()
        createComplaintUseCase = CreateComplaintUseCase(fakeRepository)
        viewModel = CreateComplaintViewModel(createComplaintUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun validation_rejectsBlankCategory() = runTest(testDispatcher) {
        viewModel.onEvent(CreateComplaintEvent.OnDescriptionChanged("Some valid description"))
        viewModel.onEvent(CreateComplaintEvent.Submit)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertNotNull(state.categoryError)
        assertFalse(state.isSuccess)
        assertFalse(state.isSaving)
    }

    @Test
    fun validation_rejectsBlankDescription() = runTest(testDispatcher) {
        viewModel.onEvent(CreateComplaintEvent.OnCategoryChanged("Water"))
        viewModel.onEvent(CreateComplaintEvent.Submit)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertNotNull(state.descriptionError)
        assertFalse(state.isSuccess)
        assertFalse(state.isSaving)
    }

    @Test
    fun submit_succeedsWithValidInput() = runTest(testDispatcher) {
        fakeRepository.createResult = Result.success(
            Complaint(
                id = "c_new",
                residentId = "res_1",
                communityId = "comm_1",
                flatId = "flat_1",
                category = "Water",
                description = "Kitchen tap leak",
                status = ComplaintStatus.SUBMITTED,
                createdAt = 1000L
            )
        )

        viewModel.onEvent(CreateComplaintEvent.OnCategoryChanged("Water"))
        viewModel.onEvent(CreateComplaintEvent.OnDescriptionChanged("Kitchen tap leak"))
        viewModel.onEvent(CreateComplaintEvent.Submit)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isSaving)
        assertTrue(state.isSuccess)
        assertNull(state.categoryError)
        assertNull(state.descriptionError)
        assertNull(state.generalError)
    }

    @Test
    fun submit_handlesFailure() = runTest(testDispatcher) {
        fakeRepository.createResult = Result.failure(IllegalStateException("No active session found"))

        viewModel.onEvent(CreateComplaintEvent.OnCategoryChanged("Water"))
        viewModel.onEvent(CreateComplaintEvent.OnDescriptionChanged("Kitchen tap leak"))
        viewModel.onEvent(CreateComplaintEvent.Submit)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isSaving)
        assertFalse(state.isSuccess)
        assertEquals("No active session found", state.generalError)
    }

    private class FakeComplaintRepository : ComplaintRepository {
        var createResult: Result<Complaint> = Result.failure(NotImplementedError())

        override suspend fun getMyComplaints(): Result<List<Complaint>> = Result.success(emptyList())

        override suspend fun getComplaintDetails(complaintId: String): Result<Complaint> =
            Result.failure(NotImplementedError())

        override suspend fun createComplaint(category: String, description: String): Result<Complaint> =
            createResult

        override suspend fun getPendingComplaintsCount(): Result<Int> = Result.success(0)
    }
}
