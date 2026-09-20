package com.communityos.security

import androidx.lifecycle.SavedStateHandle
import com.communityos.security.event.SecurityVisitorDetailEvent
import com.communityos.security.viewmodel.SecurityVisitorDetailViewModel
import com.communityos.visitors.domain.CheckInVisitorUseCase
import com.communityos.visitors.domain.CheckOutVisitorUseCase
import com.communityos.visitors.domain.DenyVisitorUseCase
import com.communityos.visitors.domain.GetSecurityVisitorDetailsUseCase
import com.communityos.visitors.model.SecurityGateProfile
import com.communityos.visitors.model.SecurityVisitorDetail
import com.communityos.visitors.model.Visitor
import com.communityos.visitors.model.VisitorStatus
import com.communityos.visitors.repository.VisitorRepository
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
class SecurityVisitorDetailViewModelTest {

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
        var detailResult: Result<SecurityVisitorDetail> = Result.failure(NotImplementedError()),
        var checkInResult: Result<Visitor> = Result.failure(NotImplementedError()),
        var checkOutResult: Result<Visitor> = Result.failure(NotImplementedError()),
        var denyResult: Result<Visitor> = Result.failure(NotImplementedError())
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
        ): Result<Visitor> = Result.failure(NotImplementedError())
        override suspend fun cancelVisitor(visitorId: String): Result<Visitor> = Result.failure(NotImplementedError())
        override suspend fun getActiveVisitorsCount(): Result<Int> = Result.success(0)
        override suspend fun getSecurityVisitors(): Result<List<Visitor>> = Result.failure(NotImplementedError())
        override fun observeSecurityVisitors(): Flow<List<Visitor>> = emptyFlow()
        override suspend fun getSecurityVisitorDetails(visitorId: String): Result<SecurityVisitorDetail> = detailResult
        override suspend fun getSecurityGateProfile(): Result<SecurityGateProfile> = Result.failure(NotImplementedError())
        override suspend fun checkInVisitor(visitorId: String): Result<Visitor> = checkInResult
        override suspend fun checkOutVisitor(visitorId: String): Result<Visitor> = checkOutResult
        override suspend fun denyVisitor(visitorId: String): Result<Visitor> = denyResult
    }

    private fun createSampleDetail(status: VisitorStatus): SecurityVisitorDetail {
        return SecurityVisitorDetail(
            visitor = Visitor(
                id = "v1",
                residentId = "r1",
                communityId = "comm_1",
                flatId = "f1",
                name = "John Doe",
                phoneNumber = "9888877777",
                purpose = "Guest",
                scheduledArrivalDate = 1700000000000L,
                status = status,
                createdAt = 1000L
            ),
            residentName = "David Miller",
            residentPhone = "9876543210",
            flatNumber = "B-304",
            block = "Block B",
            communityName = "Orchard Heights"
        )
    }

    @Test
    fun loadDetails_populatesDetailState() = runTest {
        val sample = createSampleDetail(VisitorStatus.PRE_APPROVED)
        val repository = FakeVisitorRepository(detailResult = Result.success(sample))
        val savedStateHandle = SavedStateHandle(mapOf("visitorId" to "v1"))

        val viewModel = SecurityVisitorDetailViewModel(
            savedStateHandle = savedStateHandle,
            getSecurityVisitorDetailsUseCase = GetSecurityVisitorDetailsUseCase(repository),
            checkInVisitorUseCase = CheckInVisitorUseCase(repository),
            checkOutVisitorUseCase = CheckOutVisitorUseCase(repository),
            denyVisitorUseCase = DenyVisitorUseCase(repository)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.detail)
        assertEquals("John Doe", state.detail?.visitor?.name)
        assertEquals("David Miller", state.detail?.residentName)
        assertEquals("B-304", state.detail?.flatNumber)
    }

    @Test
    fun checkIn_success_updatesVisitorStatusToCheckedIn() = runTest {
        val initial = createSampleDetail(VisitorStatus.PRE_APPROVED)
        val updated = initial.visitor.copy(status = VisitorStatus.CHECKED_IN, checkInTime = 2000L)
        val repository = FakeVisitorRepository(
            detailResult = Result.success(initial),
            checkInResult = Result.success(updated)
        )
        val savedStateHandle = SavedStateHandle(mapOf("visitorId" to "v1"))

        val viewModel = SecurityVisitorDetailViewModel(
            savedStateHandle = savedStateHandle,
            getSecurityVisitorDetailsUseCase = GetSecurityVisitorDetailsUseCase(repository),
            checkInVisitorUseCase = CheckInVisitorUseCase(repository),
            checkOutVisitorUseCase = CheckOutVisitorUseCase(repository),
            denyVisitorUseCase = DenyVisitorUseCase(repository)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(SecurityVisitorDetailEvent.CheckIn)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isProcessing)
        assertEquals(VisitorStatus.CHECKED_IN, state.detail?.visitor?.status)
        assertEquals("Visitor successfully checked in", state.actionSuccessMessage)
    }

    @Test
    fun checkOut_success_updatesVisitorStatusToCheckedOut() = runTest {
        val initial = createSampleDetail(VisitorStatus.CHECKED_IN)
        val updated = initial.visitor.copy(status = VisitorStatus.CHECKED_OUT, checkOutTime = 3000L)
        val repository = FakeVisitorRepository(
            detailResult = Result.success(initial),
            checkOutResult = Result.success(updated)
        )
        val savedStateHandle = SavedStateHandle(mapOf("visitorId" to "v1"))

        val viewModel = SecurityVisitorDetailViewModel(
            savedStateHandle = savedStateHandle,
            getSecurityVisitorDetailsUseCase = GetSecurityVisitorDetailsUseCase(repository),
            checkInVisitorUseCase = CheckInVisitorUseCase(repository),
            checkOutVisitorUseCase = CheckOutVisitorUseCase(repository),
            denyVisitorUseCase = DenyVisitorUseCase(repository)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(SecurityVisitorDetailEvent.CheckOut)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isProcessing)
        assertEquals(VisitorStatus.CHECKED_OUT, state.detail?.visitor?.status)
        assertEquals("Visitor successfully checked out", state.actionSuccessMessage)
    }

    @Test
    fun deny_success_updatesVisitorStatusToDenied() = runTest {
        val initial = createSampleDetail(VisitorStatus.PRE_APPROVED)
        val updated = initial.visitor.copy(status = VisitorStatus.DENIED, verifiedBySecurityId = "sec_1")
        val repository = FakeVisitorRepository(
            detailResult = Result.success(initial),
            denyResult = Result.success(updated)
        )
        val savedStateHandle = SavedStateHandle(mapOf("visitorId" to "v1"))

        val viewModel = SecurityVisitorDetailViewModel(
            savedStateHandle = savedStateHandle,
            getSecurityVisitorDetailsUseCase = GetSecurityVisitorDetailsUseCase(repository),
            checkInVisitorUseCase = CheckInVisitorUseCase(repository),
            checkOutVisitorUseCase = CheckOutVisitorUseCase(repository),
            denyVisitorUseCase = DenyVisitorUseCase(repository)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(SecurityVisitorDetailEvent.Deny)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isProcessing)
        assertEquals(VisitorStatus.DENIED, state.detail?.visitor?.status)
        assertEquals("Visitor entry denied", state.actionSuccessMessage)
    }
}
