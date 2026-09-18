package com.communityos.profile

import com.communityos.models.UserRole
import com.communityos.profile.domain.GetProfileUseCase
import com.communityos.profile.domain.UpdateProfileUseCase
import com.communityos.profile.event.ProfileEvent
import com.communityos.profile.model.UserProfile
import com.communityos.profile.repository.ProfileRepository
import com.communityos.profile.viewmodel.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeProfileRepository: FakeProfileRepository
    private lateinit var getProfileUseCase: GetProfileUseCase
    private lateinit var updateProfileUseCase: UpdateProfileUseCase
    private lateinit var viewModel: ProfileViewModel

    private val initialProfile = UserProfile(
        id = "user_1",
        name = "Initial Resident",
        phoneNumber = "9876543210",
        email = "initial@example.com",
        communityName = "Orchard Heights",
        flatNumber = "B-304",
        role = UserRole.RESIDENT
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeProfileRepository = FakeProfileRepository(
            profileResult = Result.success(initialProfile),
            updateResult = Result.success(initialProfile)
        )
        getProfileUseCase = GetProfileUseCase(fakeProfileRepository)
        updateProfileUseCase = UpdateProfileUseCase(fakeProfileRepository)

        viewModel = ProfileViewModel(
            getProfileUseCase = getProfileUseCase,
            updateProfileUseCase = updateProfileUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeProfileRepository(
        var profileResult: Result<UserProfile>,
        var updateResult: Result<UserProfile>
    ) : ProfileRepository {
        var updateCalled = false
        var updatedName: String? = null
        var updatedEmail: String? = null

        override suspend fun getProfile(): Result<UserProfile> = profileResult

        override suspend fun updateProfile(name: String, email: String?): Result<UserProfile> {
            updateCalled = true
            updatedName = name
            updatedEmail = email
            return updateResult
        }
    }

    @Test
    fun init_loadsProfile_updatesStateWithProfile() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(initialProfile, state.profile)
        assertNull(state.errorMessage)
    }

    @Test
    fun loadProfile_failure_updatesErrorMessage() = runTest {
        fakeProfileRepository.profileResult = Result.failure(IllegalStateException("Failed to load"))
        viewModel.onEvent(ProfileEvent.LoadProfile)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("Failed to load", state.errorMessage)
    }

    @Test
    fun onEvent_StartEditing_prefillsEditFields() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ProfileEvent.StartEditing)

        val state = viewModel.state.value
        assertTrue(state.isEditing)
        assertEquals("Initial Resident", state.editName)
        assertEquals("initial@example.com", state.editEmail)
        assertNull(state.nameError)
        assertNull(state.emailError)
    }

    @Test
    fun onEvent_CancelEditing_resetsEditModeAndErrors() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(ProfileEvent.StartEditing)
        viewModel.onEvent(ProfileEvent.NameChanged("Draft Name"))
        viewModel.onEvent(ProfileEvent.CancelEditing)

        val state = viewModel.state.value
        assertFalse(state.isEditing)
        assertEquals("", state.editName)
        assertEquals("", state.editEmail)
        assertNull(state.nameError)
    }

    @Test
    fun onEvent_SaveProfile_withBlankName_setsNameErrorAndDoesNotCallRepository() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(ProfileEvent.StartEditing)
        viewModel.onEvent(ProfileEvent.NameChanged("   "))
        viewModel.onEvent(ProfileEvent.SaveProfile)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.isEditing)
        assertEquals("Name cannot be blank", state.nameError)
        assertFalse(fakeProfileRepository.updateCalled)
    }

    @Test
    fun onEvent_SaveProfile_withInvalidEmail_setsEmailErrorAndDoesNotCallRepository() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(ProfileEvent.StartEditing)
        viewModel.onEvent(ProfileEvent.NameChanged("Valid Name"))
        viewModel.onEvent(ProfileEvent.EmailChanged("invalid-email-address"))
        viewModel.onEvent(ProfileEvent.SaveProfile)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.isEditing)
        assertEquals("Invalid email format", state.emailError)
        assertFalse(fakeProfileRepository.updateCalled)
    }

    @Test
    fun onEvent_SaveProfile_success_updatesProfileAndExitsEditMode() = runTest {
        val updated = initialProfile.copy(name = "Updated Resident", email = "updated@example.com")
        fakeProfileRepository.updateResult = Result.success(updated)

        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(ProfileEvent.StartEditing)
        viewModel.onEvent(ProfileEvent.NameChanged("Updated Resident"))
        viewModel.onEvent(ProfileEvent.EmailChanged("updated@example.com"))
        viewModel.onEvent(ProfileEvent.SaveProfile)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeProfileRepository.updateCalled)
        assertEquals("Updated Resident", fakeProfileRepository.updatedName)
        assertEquals("updated@example.com", fakeProfileRepository.updatedEmail)

        val state = viewModel.state.value
        assertFalse(state.isEditing)
        assertFalse(state.isSaving)
        assertEquals(updated, state.profile)
        assertEquals("Profile updated successfully", state.successMessage)
    }

    @Test
    fun onEvent_SaveProfile_failure_keepsEditingAndEnteredValues() = runTest {
        fakeProfileRepository.updateResult = Result.failure(IllegalStateException("Database write failed"))

        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(ProfileEvent.StartEditing)
        viewModel.onEvent(ProfileEvent.NameChanged("Attempted Name"))
        viewModel.onEvent(ProfileEvent.EmailChanged("attempt@example.com"))
        viewModel.onEvent(ProfileEvent.SaveProfile)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeProfileRepository.updateCalled)
        val state = viewModel.state.value
        assertTrue(state.isEditing)
        assertFalse(state.isSaving)
        assertEquals("Attempted Name", state.editName)
        assertEquals("attempt@example.com", state.editEmail)
        assertEquals("Database write failed", state.errorMessage)
    }
}
