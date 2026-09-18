package com.communityos.authentication

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.communityos.authentication.domain.*
import com.communityos.authentication.event.AuthEvent
import com.communityos.authentication.repository.AuthRepositoryImpl
import com.communityos.authentication.viewmodel.AuthViewModel
import com.communityos.data.local.CommunityDatabase
import com.communityos.data.local.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class OtpVerificationLogicTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: CommunityDatabase
    private lateinit var authRepository: AuthRepositoryImpl
    private lateinit var authViewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CommunityDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val testFile = context.preferencesDataStoreFile("test_otp_logic_${System.nanoTime()}")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { testFile }
        )
        val sessionManager = SessionManager(testDataStore)

        authRepository = AuthRepositoryImpl(
            userDao = database.userDao(),
            communityDao = database.communityDao(),
            flatDao = database.flatDao(),
            sessionManager = sessionManager
        )
        authViewModel = AuthViewModel(
            sendOtpUseCase = SendOtpUseCase(authRepository),
            verifyOtpUseCase = VerifyOtpUseCase(authRepository),
            registerUserUseCase = RegisterUserUseCase(authRepository),
            selectCommunityUseCase = SelectCommunityUseCase(authRepository),
            verifyFlatUseCase = VerifyFlatUseCase(authRepository)
        )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun entering_six_digits_updates_otpCode_state() = runTest {
        authViewModel.onEvent(AuthEvent.OnOtpChanged("123456"))
        assertEquals("123456", authViewModel.state.value.otpCode)
    }

    @Test
    fun valid_test_otp_verifies_successfully() = runTest {
        authViewModel.onEvent(AuthEvent.OnPhoneChanged("9876543210"))
        authViewModel.onEvent(AuthEvent.SendOtp)
        testDispatcher.scheduler.advanceUntilIdle()

        authViewModel.onEvent(AuthEvent.OnOtpChanged("123456"))
        authViewModel.onEvent(AuthEvent.VerifyOtp)

        var state = authViewModel.state.value
        var attempts = 0
        while (state.currentUser == null && state.errorMessage == null && attempts < 50) {
            testDispatcher.scheduler.advanceUntilIdle()
            Thread.sleep(50)
            attempts++
            state = authViewModel.state.value
        }

        assertNull(state.errorMessage)
        assertNotNull(state.currentUser)
        assertEquals("9876543210", state.currentUser?.phoneNumber)
    }

    @Test
    fun invalid_otp_sets_error_message() = runTest {
        authViewModel.onEvent(AuthEvent.OnPhoneChanged("9876543210"))
        authViewModel.onEvent(AuthEvent.SendOtp)
        testDispatcher.scheduler.advanceUntilIdle()

        authViewModel.onEvent(AuthEvent.OnOtpChanged("999999"))
        authViewModel.onEvent(AuthEvent.VerifyOtp)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = authViewModel.state.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Incorrect OTP"))
    }
}
