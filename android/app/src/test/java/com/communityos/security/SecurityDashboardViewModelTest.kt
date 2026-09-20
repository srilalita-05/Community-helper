package com.communityos.security

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.communityos.authentication.domain.LogoutUseCase
import com.communityos.authentication.model.AuthUser
import com.communityos.authentication.repository.AuthRepository
import com.communityos.data.local.CommunityDatabase
import com.communityos.data.local.dao.CommunityDao
import com.communityos.data.local.dao.UserDao
import com.communityos.data.local.entity.CommunityEntity
import com.communityos.data.local.entity.UserEntity
import com.communityos.data.local.session.SessionManager
import com.communityos.models.UserRole
import com.communityos.security.event.SecurityDashboardEffect
import com.communityos.security.event.SecurityDashboardEvent
import com.communityos.security.state.SecurityTab
import com.communityos.security.viewmodel.SecurityDashboardViewModel
import com.communityos.visitors.domain.GetSecurityGateProfileUseCase
import com.communityos.visitors.domain.GetSecurityVisitorsUseCase
import com.communityos.visitors.model.SecurityGateProfile
import com.communityos.visitors.model.SecurityVisitorDetail
import com.communityos.visitors.model.Visitor
import com.communityos.visitors.model.VisitorStatus
import com.communityos.visitors.repository.VisitorRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SecurityDashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: CommunityDatabase
    private lateinit var userDao: UserDao
    private lateinit var communityDao: CommunityDao
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CommunityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = database.userDao()
        communityDao = database.communityDao()

        val testFile = context.preferencesDataStoreFile("test_sec_vm_${System.nanoTime()}")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(testDispatcher + SupervisorJob()),
            produceFile = { testFile }
        )
        sessionManager = SessionManager(testDataStore)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private class FakeVisitorRepository(
        var securityVisitorsResult: Result<List<Visitor>> = Result.success(emptyList()),
        var gateProfileResult: Result<SecurityGateProfile> = Result.success(
            SecurityGateProfile("Officer Guard", "Orchard Heights")
        )
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
        override suspend fun getSecurityGateProfile(): Result<SecurityGateProfile> = gateProfileResult
        override suspend fun getSecurityVisitors(): Result<List<Visitor>> = securityVisitorsResult
        override fun observeSecurityVisitors(): Flow<List<Visitor>> = emptyFlow()
        override suspend fun getSecurityVisitorDetails(visitorId: String): Result<SecurityVisitorDetail> = Result.failure(NotImplementedError())
        override suspend fun checkInVisitor(visitorId: String): Result<Visitor> = Result.failure(NotImplementedError())
        override suspend fun checkOutVisitor(visitorId: String): Result<Visitor> = Result.failure(NotImplementedError())
        override suspend fun denyVisitor(visitorId: String): Result<Visitor> = Result.failure(NotImplementedError())
    }

    private class FakeAuthRepository : AuthRepository {
        var loggedOut = false
        override suspend fun sendOtp(phoneNumber: String): Result<Unit> = Result.success(Unit)
        override suspend fun verifyOtp(code: String): Result<AuthUser> = Result.failure(NotImplementedError())
        override suspend fun registerUser(name: String, email: String): Result<AuthUser> = Result.failure(NotImplementedError())
        override suspend fun selectCommunity(communityName: String): Result<Unit> = Result.success(Unit)
        override suspend fun verifyFlat(flatNo: String, role: String): Result<Unit> = Result.success(Unit)
        override suspend fun restoreSession(): Result<AuthUser?> = Result.success(null)
        override suspend fun logout(): Result<Unit> {
            loggedOut = true
            return Result.success(Unit)
        }
    }

    @Test
    fun loadDashboard_populatesOfficerAndCommunityAndVisitors() = runTest {
        val visitors = listOf(
            Visitor("v1", "r1", "comm_1", "f1", "Expected Vis", "9888877771", "Guest", null, 1700000000000L, VisitorStatus.PRE_APPROVED, null, null, null, null, 1000L),
            Visitor("v2", "r1", "comm_1", "f1", "Inside Vis", "9888877772", "Delivery", null, 1700000000000L, VisitorStatus.CHECKED_IN, null, 1500L, null, "sec_1", 1000L),
            Visitor("v3", "r1", "comm_1", "f1", "Exited Vis", "9888877773", "Cab", null, 1700000000000L, VisitorStatus.CHECKED_OUT, null, 1500L, 2500L, "sec_1", 1000L)
        )
        val visitorRepository = FakeVisitorRepository(securityVisitorsResult = Result.success(visitors))
        val getSecurityVisitorsUseCase = GetSecurityVisitorsUseCase(visitorRepository)
        val getSecurityGateProfileUseCase = GetSecurityGateProfileUseCase(visitorRepository)
        val authRepository = FakeAuthRepository()
        val logoutUseCase = LogoutUseCase(authRepository)

        val viewModel = SecurityDashboardViewModel(
            getSecurityVisitorsUseCase,
            getSecurityGateProfileUseCase,
            logoutUseCase
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Officer Guard", state.officerName)
        assertEquals("Orchard Heights", state.communityName)
        assertEquals(3, state.allVisitors.size)
        assertEquals(1, state.expectedVisitors.size)
        assertEquals(1, state.checkedInVisitors.size)
        assertEquals(1, state.historyVisitors.size)

        // Default tab is EXPECTED
        assertEquals(1, state.displayedVisitors.size)
        assertEquals("Expected Vis", state.displayedVisitors[0].name)

        // Select CHECKED_IN tab
        viewModel.onEvent(SecurityDashboardEvent.SelectTab(SecurityTab.CHECKED_IN))
        val checkedInState = viewModel.state.value
        assertEquals(1, checkedInState.displayedVisitors.size)
        assertEquals("Inside Vis", checkedInState.displayedVisitors[0].name)

        // Select HISTORY tab
        viewModel.onEvent(SecurityDashboardEvent.SelectTab(SecurityTab.HISTORY))
        val historyState = viewModel.state.value
        assertEquals(1, historyState.displayedVisitors.size)
        assertEquals("Exited Vis", historyState.displayedVisitors[0].name)
    }

    @Test
    fun logout_callsLogoutUseCaseAndEmitsEffect() = runTest {
        val visitorRepository = FakeVisitorRepository()
        val getSecurityVisitorsUseCase = GetSecurityVisitorsUseCase(visitorRepository)
        val getSecurityGateProfileUseCase = GetSecurityGateProfileUseCase(visitorRepository)
        val authRepository = FakeAuthRepository()
        val logoutUseCase = LogoutUseCase(authRepository)

        val viewModel = SecurityDashboardViewModel(
            getSecurityVisitorsUseCase,
            getSecurityGateProfileUseCase,
            logoutUseCase
        )

        var emittedEffect: SecurityDashboardEffect? = null
        val job = launch {
            emittedEffect = viewModel.effect.first()
        }

        viewModel.onEvent(SecurityDashboardEvent.Logout)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(authRepository.loggedOut)
        assertEquals(SecurityDashboardEffect.NavigateToOnboarding, emittedEffect)
        job.cancel()
    }
}
