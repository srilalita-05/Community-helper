package com.communityos.visitors

import androidx.lifecycle.SavedStateHandle
import com.communityos.visitors.domain.CancelVisitorUseCase
import com.communityos.visitors.domain.GetResidentVisitorDetailsUseCase
import com.communityos.visitors.event.ResidentVisitorDetailEvent
import com.communityos.visitors.model.SecurityGateProfile
import com.communityos.visitors.model.SecurityVisitorDetail
import com.communityos.visitors.model.Visitor
import com.communityos.visitors.model.VisitorStatus
import com.communityos.visitors.repository.VisitorRepository
import com.communityos.visitors.viewmodel.ResidentVisitorDetailViewModel
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
class ResidentVisitorDetailViewModelTest {

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
        var detailResult: Result<Visitor> = Result.failure(NotImplementedError()),
        var cancelResult: Result<Visitor> = Result.failure(NotImplementedError())
    ) : VisitorRepository {
        override suspend fun getMyVisitors(): Result<List<Visitor>> = Result.failure(NotImplementedError())
        override fun observeMyVisitors(): Flow<List<Visitor>> = emptyFlow()
        override suspend fun getResidentVisitorDetails(visitorId: String): Result<Visitor> = detailResult
        override suspend fun createVisitor(
            name: String,
            phoneNumber: String,
            purpose: String,
            scheduledArrivalDate: Long,
            vehicleNumber: String?
        ): Result<Visitor> = Result.failure(NotImplementedError())
        override suspend fun cancelVisitor(visitorId: String): Result<Visitor> = cancelResult
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
    fun loadDetails_populatesState() = runTest {
        val dummy = Visitor(
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
        val repository = FakeVisitorRepository(detailResult = Result.success(dummy))
        val getDetailsUseCase = GetResidentVisitorDetailsUseCase(repository)
        val cancelUseCase = CancelVisitorUseCase(repository)
        val savedStateHandle = SavedStateHandle(mapOf("visitorId" to "v1"))

        val viewModel = ResidentVisitorDetailViewModel(savedStateHandle, getDetailsUseCase, cancelUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.visitor)
        assertEquals("John Doe", state.visitor?.name)
        assertNull(state.error)
    }

    @Test
    fun cancelPass_success_updatesStatusToCancelled() = runTest {
        val dummy = Visitor(
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
        val cancelled = dummy.copy(status = VisitorStatus.CANCELLED, updatedAt = 2000L)
        val repository = FakeVisitorRepository(
            detailResult = Result.success(dummy),
            cancelResult = Result.success(cancelled)
        )
        val getDetailsUseCase = GetResidentVisitorDetailsUseCase(repository)
        val cancelUseCase = CancelVisitorUseCase(repository)
        val savedStateHandle = SavedStateHandle(mapOf("visitorId" to "v1"))

        val viewModel = ResidentVisitorDetailViewModel(savedStateHandle, getDetailsUseCase, cancelUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ResidentVisitorDetailEvent.CancelPass)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isCancelling)
        assertTrue(state.cancelSuccess)
        assertEquals(VisitorStatus.CANCELLED, state.visitor?.status)
    }
}
