package com.communityos.home

import com.communityos.authentication.domain.LogoutUseCase
import com.communityos.authentication.model.AuthUser
import com.communityos.authentication.repository.AuthRepository
import com.communityos.home.domain.GetDashboardDataUseCase
import com.communityos.home.domain.TriggerEmergencyAlertUseCase
import com.communityos.home.event.HomeEffect
import com.communityos.home.event.HomeEvent
import com.communityos.home.model.DashboardSummary
import com.communityos.home.repository.HomeRepository
import com.communityos.home.viewmodel.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeHomeRepository: FakeHomeRepository
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var getDashboardDataUseCase: GetDashboardDataUseCase
    private lateinit var triggerEmergencyAlertUseCase: TriggerEmergencyAlertUseCase
    private lateinit var logoutUseCase: LogoutUseCase
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeHomeRepository = FakeHomeRepository()
        fakeAuthRepository = FakeAuthRepository()
        getDashboardDataUseCase = GetDashboardDataUseCase(fakeHomeRepository)
        triggerEmergencyAlertUseCase = TriggerEmergencyAlertUseCase(fakeHomeRepository)
        logoutUseCase = LogoutUseCase(fakeAuthRepository)

        viewModel = HomeViewModel(
            getDashboardDataUseCase = getDashboardDataUseCase,
            triggerEmergencyAlertUseCase = triggerEmergencyAlertUseCase,
            logoutUseCase = logoutUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeHomeRepository : HomeRepository {
        var summaryResult: Result<DashboardSummary> = Result.success(
            DashboardSummary(
                activeVisitors = 0,
                pendingComplaints = 0,
                outstandingDues = 0.0,
                communityName = "Test Community",
                blockNo = "A",
                flatNo = "101"
            )
        )

        override suspend fun getDashboardSummary(): Result<DashboardSummary> = summaryResult
        override suspend fun triggerEmergencyAlert(type: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeAuthRepository : AuthRepository {
        var logoutResult: Result<Unit> = Result.success(Unit)
        var logoutCalled = false

        override suspend fun sendOtp(phoneNumber: String): Result<Unit> = Result.success(Unit)
        override suspend fun verifyOtp(code: String): Result<AuthUser> = Result.failure(NotImplementedError())
        override suspend fun registerUser(name: String, email: String): Result<AuthUser> = Result.failure(NotImplementedError())
        override suspend fun selectCommunity(communityName: String): Result<Unit> = Result.success(Unit)
        override suspend fun verifyFlat(flatNo: String, role: String): Result<Unit> = Result.success(Unit)
        override suspend fun restoreSession(): Result<AuthUser?> = Result.success(null)

        override suspend fun logout(): Result<Unit> {
            logoutCalled = true
            return logoutResult
        }
    }

    @Test
    fun onEvent_logout_invokesLogoutUseCaseAndEmitsNavigateToOnboardingEffect() = runTest {
        var receivedEffect: HomeEffect? = null
        val collectJob = launch(testDispatcher) {
            receivedEffect = viewModel.effect.first()
        }

        viewModel.onEvent(HomeEvent.Logout)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeAuthRepository.logoutCalled)
        assertEquals(HomeEffect.NavigateToOnboarding, receivedEffect)
        collectJob.cancel()
    }

    @Test
    fun onEvent_logout_whenFails_updatesErrorMessageAndDoesNotEmitEffect() = runTest {
        fakeAuthRepository.logoutResult = Result.failure(IllegalStateException("Logout failure"))

        var effectReceived = false
        val collectJob = launch(testDispatcher) {
            viewModel.effect.collect {
                effectReceived = true
            }
        }

        viewModel.onEvent(HomeEvent.Logout)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeAuthRepository.logoutCalled)
        assertFalse(effectReceived)
        assertEquals("Logout failure", viewModel.state.value.errorMessage)
        collectJob.cancel()
    }
}
