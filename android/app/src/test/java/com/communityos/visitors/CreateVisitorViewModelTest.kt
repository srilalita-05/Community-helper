package com.communityos.visitors

import com.communityos.visitors.domain.CreateVisitorUseCase
import com.communityos.visitors.event.CreateVisitorEvent
import com.communityos.visitors.model.SecurityGateProfile
import com.communityos.visitors.model.SecurityVisitorDetail
import com.communityos.visitors.model.Visitor
import com.communityos.visitors.model.VisitorStatus
import com.communityos.visitors.repository.VisitorRepository
import com.communityos.visitors.viewmodel.CreateVisitorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateVisitorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeVisitorRepository(
        var createResult: Result<Visitor> = Result.failure(NotImplementedError())
    ) : VisitorRepository {
        override suspend fun getMyVisitors(): Result<List<Visitor>> = Result.failure(NotImplementedError())
        override fun observeMyVisitors(): Flow<List<Visitor>> = emptyFlow()
        override suspend fun getResidentVisitorDetails(visitorId: String): Result<Visitor> = Result.failure(NotImplementedError())
        override suspend fun createVisitor(
            name: String,
            phoneNumber: String,
            purpose: String,
            scheduledArrivalDate: Long,
            vehicleNumber: String?
        ): Result<Visitor> = createResult
        override suspend fun cancelVisitor(visitorId: String): Result<Visitor> = Result.failure(NotImplementedError())
        override suspend fun getActiveVisitorsCount(): Result<Int> = Result.success(0)
        override suspend fun getSecurityVisitors(): Result<List<Visitor>> = Result.failure(NotImplementedError())
        override fun observeSecurityVisitors(): Flow<List<Visitor>> = emptyFlow()
        override suspend fun getSecurityVisitorDetails(visitorId: String): Result<SecurityVisitorDetail> = Result.failure(NotImplementedError())
        override suspend fun getSecurityGateProfile(): Result<SecurityGateProfile> = Result.failure(NotImplementedError())
        override suspend fun checkInVisitor(visitorId: String): Result<Visitor> = Result.failure(NotImplementedError())
        override suspend fun checkOutVisitor(visitorId: String): Result<Visitor> = Result.failure(NotImplementedError())
        override suspend fun denyVisitor(visitorId: String): Result<Visitor> = Result.failure(NotImplementedError())
    }

    @Test
    fun validation_emptyFields_setsAppropriateErrors() = runTest {
        val repository = FakeVisitorRepository()
        val useCase = CreateVisitorUseCase(repository)
        val viewModel = CreateVisitorViewModel(useCase)

        // Submit with default empty name/phone
        viewModel.onEvent(CreateVisitorEvent.Submit)

        val state = viewModel.state.value
        assertNotNull(state.nameError)
        assertNotNull(state.phoneError)
        assertFalse(state.isSuccess)
    }

    @Test
    fun validation_invalidPhone_setsPhoneError() = runTest {
        val repository = FakeVisitorRepository()
        val useCase = CreateVisitorUseCase(repository)
        val viewModel = CreateVisitorViewModel(useCase)

        viewModel.onEvent(CreateVisitorEvent.OnNameChanged("John Doe"))
        viewModel.onEvent(CreateVisitorEvent.OnPhoneChanged("12345")) // invalid
        viewModel.onEvent(CreateVisitorEvent.OnPurposeChanged("Guest"))
        viewModel.onEvent(CreateVisitorEvent.Submit)

        val state = viewModel.state.value
        assertNull(state.nameError)
        assertNotNull(state.phoneError)
        assertFalse(state.isSuccess)
    }

    @Test
    fun submit_validFields_triggersUseCaseAndSetsSuccess() = runTest {
        val dummyVisitor = Visitor(
            id = "v1",
            residentId = "res1",
            communityId = "c1",
            flatId = "f1",
            name = "John Doe",
            phoneNumber = "9888877777",
            purpose = "Guest",
            scheduledArrivalDate = 1700000000000L,
            status = VisitorStatus.PRE_APPROVED,
            createdAt = 1000L
        )
        val repository = FakeVisitorRepository(createResult = Result.success(dummyVisitor))
        val useCase = CreateVisitorUseCase(repository)
        val viewModel = CreateVisitorViewModel(useCase)

        viewModel.onEvent(CreateVisitorEvent.OnNameChanged("John Doe"))
        viewModel.onEvent(CreateVisitorEvent.OnPhoneChanged("9888877777"))
        viewModel.onEvent(CreateVisitorEvent.OnPurposeChanged("Guest"))
        viewModel.onEvent(CreateVisitorEvent.OnVehicleChanged("KA-05-MJ-1234"))
        viewModel.onEvent(CreateVisitorEvent.Submit)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isSaving)
        assertTrue(state.isSuccess)
        assertNull(state.generalError)
    }

    @Test
    fun submit_repositoryFailure_setsGeneralError() = runTest {
        val repository = FakeVisitorRepository(createResult = Result.failure(IllegalStateException("Failed to insert")))
        val useCase = CreateVisitorUseCase(repository)
        val viewModel = CreateVisitorViewModel(useCase)

        viewModel.onEvent(CreateVisitorEvent.OnNameChanged("John Doe"))
        viewModel.onEvent(CreateVisitorEvent.OnPhoneChanged("9888877777"))
        viewModel.onEvent(CreateVisitorEvent.OnPurposeChanged("Guest"))
        viewModel.onEvent(CreateVisitorEvent.Submit)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isSaving)
        assertFalse(state.isSuccess)
        assertEquals("Failed to insert", state.generalError)
    }
}
