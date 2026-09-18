package com.communityos.authentication

import com.communityos.authentication.domain.RestoreSessionUseCase
import com.communityos.authentication.model.AuthUser
import com.communityos.authentication.repository.AuthRepository
import com.communityos.authentication.viewmodel.SplashViewModel
import com.communityos.authentication.viewmodel.StartupDestination
import com.communityos.models.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeAuthRepository(
        var sessionResult: Result<AuthUser?> = Result.success(null)
    ) : AuthRepository {
        override suspend fun sendOtp(phoneNumber: String): Result<Unit> = Result.success(Unit)
        override suspend fun verifyOtp(code: String): Result<AuthUser> = Result.failure(NotImplementedError())
        override suspend fun registerUser(name: String, email: String): Result<AuthUser> = Result.failure(NotImplementedError())
        override suspend fun selectCommunity(communityName: String): Result<Unit> = Result.success(Unit)
        override suspend fun verifyFlat(flatNo: String, role: String): Result<Unit> = Result.success(Unit)
        override suspend fun restoreSession(): Result<AuthUser?> = sessionResult
    }

    @Test
    fun checkSession_withValidSession_setsDestinationHome() = runTest {
        val fakeUser = AuthUser(
            uid = "user_123",
            phoneNumber = "9876543210",
            displayName = "Test User",
            role = UserRole.RESIDENT
        )
        val repository = FakeAuthRepository(sessionResult = Result.success(fakeUser))
        val useCase = RestoreSessionUseCase(repository)

        val viewModel = SplashViewModel(useCase)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(StartupDestination.HOME, viewModel.destination.value)
    }

    @Test
    fun checkSession_withNoSession_setsDestinationAuth() = runTest {
        val repository = FakeAuthRepository(sessionResult = Result.success(null))
        val useCase = RestoreSessionUseCase(repository)

        val viewModel = SplashViewModel(useCase)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(StartupDestination.AUTH, viewModel.destination.value)
    }

    @Test
    fun checkSession_withFailedRestoration_setsDestinationAuth() = runTest {
        val repository = FakeAuthRepository(sessionResult = Result.failure(IllegalStateException("Room error")))
        val useCase = RestoreSessionUseCase(repository)

        val viewModel = SplashViewModel(useCase)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(StartupDestination.AUTH, viewModel.destination.value)
    }
}
