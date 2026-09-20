package com.communityos.visitors

import com.communityos.visitors.domain.GetMyVisitorsUseCase
import com.communityos.visitors.event.ResidentVisitorsEvent
import com.communityos.visitors.model.SecurityGateProfile
import com.communityos.visitors.model.SecurityVisitorDetail
import com.communityos.visitors.model.Visitor
import com.communityos.visitors.model.VisitorStatus
import com.communityos.visitors.repository.VisitorRepository
import com.communityos.visitors.state.VisitorFilter
import com.communityos.visitors.viewmodel.ResidentVisitorsViewModel
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
class ResidentVisitorsViewModelTest {

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
        var myVisitorsResult: Result<List<Visitor>> = Result.success(emptyList())
    ) : VisitorRepository {
        override suspend fun getMyVisitors(): Result<List<Visitor>> = myVisitorsResult
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
        override suspend fun getSecurityVisitorDetails(visitorId: String): Result<SecurityVisitorDetail> = Result.failure(NotImplementedError())
        override suspend fun getSecurityGateProfile(): Result<SecurityGateProfile> = Result.failure(NotImplementedError())
        override suspend fun checkInVisitor(visitorId: String): Result<Visitor> = Result.failure(NotImplementedError())
        override suspend fun checkOutVisitor(visitorId: String): Result<Visitor> = Result.failure(NotImplementedError())
        override suspend fun denyVisitor(visitorId: String): Result<Visitor> = Result.failure(NotImplementedError())
    }

    @Test
    fun loadVisitors_initiallyLoadsAndSetsPopulatedState() = runTest {
        val visitors = listOf(
            Visitor(
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
        )
        val repository = FakeVisitorRepository(myVisitorsResult = Result.success(visitors))
        val useCase = GetMyVisitorsUseCase(repository)

        val viewModel = ResidentVisitorsViewModel(useCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(1, state.visitors.size)
        assertEquals("John Doe", state.visitors[0].name)
    }

    @Test
    fun loadVisitors_whenFails_setsErrorState() = runTest {
        val repository = FakeVisitorRepository(myVisitorsResult = Result.failure(IllegalStateException("Network failure")))
        val useCase = GetMyVisitorsUseCase(repository)

        val viewModel = ResidentVisitorsViewModel(useCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("Network failure", state.error)
        assertTrue(state.visitors.isEmpty())
    }

    @Test
    fun filterVisitors_filtersActiveAndPastCorrectly() = runTest {
        val activeVisitor = Visitor(
            id = "v1",
            residentId = "res1",
            communityId = "c1",
            flatId = "f1",
            name = "Active",
            phoneNumber = "9888877771",
            purpose = "Guest",
            scheduledArrivalDate = 1700000000000L,
            status = VisitorStatus.PRE_APPROVED,
            createdAt = 1000L
        )
        val pastVisitor = Visitor(
            id = "v2",
            residentId = "res1",
            communityId = "c1",
            flatId = "f1",
            name = "Past",
            phoneNumber = "9888877772",
            purpose = "Delivery",
            scheduledArrivalDate = 1700000000000L,
            status = VisitorStatus.CHECKED_OUT,
            createdAt = 2000L
        )
        val repository = FakeVisitorRepository(myVisitorsResult = Result.success(listOf(activeVisitor, pastVisitor)))
        val useCase = GetMyVisitorsUseCase(repository)

        val viewModel = ResidentVisitorsViewModel(useCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // All filter
        assertEquals(2, viewModel.state.value.filteredVisitors.size)

        // Active filter
        viewModel.onEvent(ResidentVisitorsEvent.SetFilter(VisitorFilter.ACTIVE))
        assertEquals(1, viewModel.state.value.filteredVisitors.size)
        assertEquals("Active", viewModel.state.value.filteredVisitors[0].name)

        // Past filter
        viewModel.onEvent(ResidentVisitorsEvent.SetFilter(VisitorFilter.PAST))
        assertEquals(1, viewModel.state.value.filteredVisitors.size)
        assertEquals("Past", viewModel.state.value.filteredVisitors[0].name)
    }
}
