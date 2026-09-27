package com.communityos.marketplace

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.communityos.data.local.CommunityDatabase
import com.communityos.data.local.dao.UserDao
import com.communityos.data.local.entity.UserEntity
import com.communityos.data.local.session.SessionManager
import com.communityos.marketplace.domain.CreateListingUseCase
import com.communityos.marketplace.domain.GetListingDetailsUseCase
import com.communityos.marketplace.domain.UpdateListingUseCase
import com.communityos.marketplace.event.CreateEditListingEvent
import com.communityos.marketplace.model.ListingStatus
import com.communityos.marketplace.model.MarketplaceCategory
import com.communityos.marketplace.model.MarketplaceListing
import com.communityos.marketplace.model.MarketplaceListingDetail
import com.communityos.marketplace.viewmodel.CreateEditListingViewModel
import com.communityos.models.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
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

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class CreateEditListingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: CommunityDatabase
    private lateinit var userDao: UserDao
    private lateinit var sessionManager: SessionManager
    private lateinit var fakeRepository: FakeMarketplaceRepository
    private lateinit var createListingUseCase: CreateListingUseCase
    private lateinit var updateListingUseCase: UpdateListingUseCase
    private lateinit var getListingDetailsUseCase: GetListingDetailsUseCase

    @Before
    fun setup() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)

        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CommunityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = database.userDao()

        val testFile = context.preferencesDataStoreFile("test_create_edit_${System.nanoTime()}")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { testFile }
        )
        sessionManager = SessionManager(testDataStore)

        // Seed current user
        userDao.insertUser(
            UserEntity(
                id = "res_1",
                phoneNumber = "9876543210",
                name = "Alice",
                email = "alice@example.com",
                role = UserRole.RESIDENT,
                communityId = "comm_1",
                flatId = null,
                isApproved = true,
                createdAt = 1000L
            )
        )
        sessionManager.saveSession("res_1", UserRole.RESIDENT)

        fakeRepository = FakeMarketplaceRepository()
        createListingUseCase = CreateListingUseCase(fakeRepository)
        updateListingUseCase = UpdateListingUseCase(fakeRepository)
        getListingDetailsUseCase = GetListingDetailsUseCase(fakeRepository)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun createMode_prefillsResidentPhoneNumber() = runTest {
        val viewModel = CreateEditListingViewModel(
            savedStateHandle = SavedStateHandle(),
            createListingUseCase = createListingUseCase,
            updateListingUseCase = updateListingUseCase,
            getListingDetailsUseCase = getListingDetailsUseCase,
            sessionManager = sessionManager,
            userDao = userDao
        )
        val state = viewModel.state.first { it.contactPhone.isNotBlank() }
        assertFalse(state.isEditMode)
        assertEquals("9876543210", state.contactPhone)
    }

    @Test
    fun editMode_loadsExistingListing() = runTest(testDispatcher) {
        val existingDetail = MarketplaceListingDetail(
            listing = MarketplaceListing(
                id = "edit_item_1",
                residentId = "res_1",
                communityId = "comm_1",
                title = "Wooden Chair",
                description = "Teak wood chair",
                category = MarketplaceCategory.FURNITURE,
                price = 1200.0,
                contactPhone = "9876543210",
                status = ListingStatus.ACTIVE,
                imageUri = null,
                createdAt = 1000L,
                updatedAt = null
            ),
            sellerName = "Alice",
            flatNumber = "A-101",
            block = "Block A",
            contactPhone = "9876543210",
            isOwner = true
        )
        fakeRepository.listingDetailsResult = Result.success(existingDetail)

        val viewModel = CreateEditListingViewModel(
            savedStateHandle = SavedStateHandle(mapOf("listingId" to "edit_item_1")),
            createListingUseCase = createListingUseCase,
            updateListingUseCase = updateListingUseCase,
            getListingDetailsUseCase = getListingDetailsUseCase,
            sessionManager = sessionManager,
            userDao = userDao
        )
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.isEditMode)
        assertEquals("Wooden Chair", state.title)
        assertEquals("Teak wood chair", state.description)
        assertEquals("1200", state.priceText)
        assertEquals(MarketplaceCategory.FURNITURE, state.category)
        assertEquals(ListingStatus.ACTIVE, state.status)
    }

    @Test
    fun validation_blankTitle_showsError() = runTest(testDispatcher) {
        val viewModel = CreateEditListingViewModel(
            savedStateHandle = SavedStateHandle(),
            createListingUseCase = createListingUseCase,
            updateListingUseCase = updateListingUseCase,
            getListingDetailsUseCase = getListingDetailsUseCase,
            sessionManager = sessionManager,
            userDao = userDao
        )
        testScheduler.advanceUntilIdle()

        viewModel.onEvent(CreateEditListingEvent.OnTitleChanged("   "))
        viewModel.onEvent(CreateEditListingEvent.OnDescriptionChanged("Valid description"))
        viewModel.onEvent(CreateEditListingEvent.OnPriceChanged("500"))
        viewModel.onEvent(CreateEditListingEvent.Submit)

        val state = viewModel.state.value
        assertNotNull(state.titleError)
        assertEquals("Title cannot be empty", state.titleError)
        assertFalse(state.isSuccess)
    }

    @Test
    fun validation_blankDescription_showsError() = runTest(testDispatcher) {
        val viewModel = CreateEditListingViewModel(
            savedStateHandle = SavedStateHandle(),
            createListingUseCase = createListingUseCase,
            updateListingUseCase = updateListingUseCase,
            getListingDetailsUseCase = getListingDetailsUseCase,
            sessionManager = sessionManager,
            userDao = userDao
        )
        testScheduler.advanceUntilIdle()

        viewModel.onEvent(CreateEditListingEvent.OnTitleChanged("Valid Title"))
        viewModel.onEvent(CreateEditListingEvent.OnDescriptionChanged("   "))
        viewModel.onEvent(CreateEditListingEvent.OnPriceChanged("500"))
        viewModel.onEvent(CreateEditListingEvent.Submit)

        val state = viewModel.state.value
        assertNotNull(state.descriptionError)
        assertEquals("Description cannot be empty", state.descriptionError)
        assertFalse(state.isSuccess)
    }

    @Test
    fun validation_invalidPrice_showsError() = runTest(testDispatcher) {
        val viewModel = CreateEditListingViewModel(
            savedStateHandle = SavedStateHandle(),
            createListingUseCase = createListingUseCase,
            updateListingUseCase = updateListingUseCase,
            getListingDetailsUseCase = getListingDetailsUseCase,
            sessionManager = sessionManager,
            userDao = userDao
        )
        testScheduler.advanceUntilIdle()

        viewModel.onEvent(CreateEditListingEvent.OnTitleChanged("Valid Title"))
        viewModel.onEvent(CreateEditListingEvent.OnDescriptionChanged("Valid description"))

        // Test <= 0
        viewModel.onEvent(CreateEditListingEvent.OnPriceChanged("0"))
        viewModel.onEvent(CreateEditListingEvent.Submit)
        assertEquals("Price must be greater than 0", viewModel.state.value.priceError)

        // Test non-numeric
        viewModel.onEvent(CreateEditListingEvent.OnPriceChanged("abc"))
        viewModel.onEvent(CreateEditListingEvent.Submit)
        assertEquals("Price must be greater than 0", viewModel.state.value.priceError)
    }

    @Test
    fun validation_invalidPhone_showsError() = runTest(testDispatcher) {
        val viewModel = CreateEditListingViewModel(
            savedStateHandle = SavedStateHandle(),
            createListingUseCase = createListingUseCase,
            updateListingUseCase = updateListingUseCase,
            getListingDetailsUseCase = getListingDetailsUseCase,
            sessionManager = sessionManager,
            userDao = userDao
        )
        testScheduler.advanceUntilIdle()

        viewModel.onEvent(CreateEditListingEvent.OnTitleChanged("Valid Title"))
        viewModel.onEvent(CreateEditListingEvent.OnDescriptionChanged("Valid description"))
        viewModel.onEvent(CreateEditListingEvent.OnPriceChanged("500"))

        // Test invalid phone format
        viewModel.onEvent(CreateEditListingEvent.OnPhoneChanged("12345"))
        viewModel.onEvent(CreateEditListingEvent.Submit)
        assertEquals("Enter a valid 10-digit mobile number", viewModel.state.value.phoneError)

        // Test blank phone
        viewModel.onEvent(CreateEditListingEvent.OnPhoneChanged(""))
        viewModel.onEvent(CreateEditListingEvent.Submit)
        assertEquals("Contact phone cannot be empty", viewModel.state.value.phoneError)
    }

    @Test
    fun submit_createSuccess_setsIsSuccess() = runTest(testDispatcher) {
        val viewModel = CreateEditListingViewModel(
            savedStateHandle = SavedStateHandle(),
            createListingUseCase = createListingUseCase,
            updateListingUseCase = updateListingUseCase,
            getListingDetailsUseCase = getListingDetailsUseCase,
            sessionManager = sessionManager,
            userDao = userDao
        )
        testScheduler.advanceUntilIdle()

        viewModel.onEvent(CreateEditListingEvent.OnTitleChanged("Microwave"))
        viewModel.onEvent(CreateEditListingEvent.OnDescriptionChanged("Like new condition"))
        viewModel.onEvent(CreateEditListingEvent.OnCategoryChanged(MarketplaceCategory.APPLIANCES))
        viewModel.onEvent(CreateEditListingEvent.OnPriceChanged("3500"))
        viewModel.onEvent(CreateEditListingEvent.OnPhoneChanged("9876543210"))
        viewModel.onEvent(CreateEditListingEvent.Submit)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.isSuccess)
        assertNull(state.errorMessage)
    }

    @Test
    fun submit_editSuccess_setsIsSuccess() = runTest(testDispatcher) {
        val existingDetail = MarketplaceListingDetail(
            listing = MarketplaceListing(
                id = "item_1",
                residentId = "res_1",
                communityId = "comm_1",
                title = "Old Title",
                description = "Old Desc",
                category = MarketplaceCategory.OTHER,
                price = 100.0,
                contactPhone = "9876543210",
                status = ListingStatus.ACTIVE,
                imageUri = null,
                createdAt = 1000L,
                updatedAt = null
            ),
            sellerName = "Alice",
            flatNumber = "A-101",
            block = "Block A",
            contactPhone = "9876543210",
            isOwner = true
        )
        fakeRepository.listingDetailsResult = Result.success(existingDetail)

        val viewModel = CreateEditListingViewModel(
            savedStateHandle = SavedStateHandle(mapOf("listingId" to "item_1")),
            createListingUseCase = createListingUseCase,
            updateListingUseCase = updateListingUseCase,
            getListingDetailsUseCase = getListingDetailsUseCase,
            sessionManager = sessionManager,
            userDao = userDao
        )
        testScheduler.advanceUntilIdle()

        viewModel.onEvent(CreateEditListingEvent.OnTitleChanged("New Title"))
        viewModel.onEvent(CreateEditListingEvent.OnPriceChanged("250"))
        viewModel.onEvent(CreateEditListingEvent.Submit)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.isSuccess)
    }

    @Test
    fun submit_failure_setsErrorMessage() = runTest(testDispatcher) {
        fakeRepository.createListingResult = Result.failure(RuntimeException("Network or database error"))

        val viewModel = CreateEditListingViewModel(
            savedStateHandle = SavedStateHandle(),
            createListingUseCase = createListingUseCase,
            updateListingUseCase = updateListingUseCase,
            getListingDetailsUseCase = getListingDetailsUseCase,
            sessionManager = sessionManager,
            userDao = userDao
        )
        testScheduler.advanceUntilIdle()

        viewModel.onEvent(CreateEditListingEvent.OnTitleChanged("Item"))
        viewModel.onEvent(CreateEditListingEvent.OnDescriptionChanged("Description"))
        viewModel.onEvent(CreateEditListingEvent.OnPriceChanged("100"))
        viewModel.onEvent(CreateEditListingEvent.OnPhoneChanged("9876543210"))
        viewModel.onEvent(CreateEditListingEvent.Submit)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertEquals("Network or database error", state.errorMessage)
    }
}
