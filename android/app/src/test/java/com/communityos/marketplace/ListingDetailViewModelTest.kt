package com.communityos.marketplace

import androidx.lifecycle.SavedStateHandle
import com.communityos.marketplace.domain.DeleteListingUseCase
import com.communityos.marketplace.domain.GetListingDetailsUseCase
import com.communityos.marketplace.domain.UpdateListingUseCase
import com.communityos.marketplace.event.ListingDetailEvent
import com.communityos.marketplace.model.ListingStatus
import com.communityos.marketplace.model.MarketplaceCategory
import com.communityos.marketplace.model.MarketplaceListing
import com.communityos.marketplace.model.MarketplaceListingDetail
import com.communityos.marketplace.viewmodel.ListingDetailViewModel
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
class ListingDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeMarketplaceRepository
    private lateinit var getListingDetailsUseCase: GetListingDetailsUseCase
    private lateinit var updateListingUseCase: UpdateListingUseCase
    private lateinit var deleteListingUseCase: DeleteListingUseCase
    private lateinit var viewModel: ListingDetailViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeMarketplaceRepository()
        getListingDetailsUseCase = GetListingDetailsUseCase(fakeRepository)
        updateListingUseCase = UpdateListingUseCase(fakeRepository)
        deleteListingUseCase = DeleteListingUseCase(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadListing_ownerDetectionAndDetailsLoaded() = runTest(testDispatcher) {
        val detail = MarketplaceListingDetail(
            listing = MarketplaceListing(
                id = "item_1",
                residentId = "res_1",
                communityId = "comm_1",
                title = "Study Desk",
                description = "Compact study desk",
                category = MarketplaceCategory.FURNITURE,
                price = 2000.0,
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
        fakeRepository.listingDetailsResult = Result.success(detail)

        val savedStateHandle = SavedStateHandle(mapOf("listingId" to "item_1"))
        viewModel = ListingDetailViewModel(
            savedStateHandle = savedStateHandle,
            getListingDetailsUseCase = getListingDetailsUseCase,
            updateListingUseCase = updateListingUseCase,
            deleteListingUseCase = deleteListingUseCase
        )
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.detail)
        assertTrue(state.detail!!.isOwner)
        assertEquals("Study Desk", state.detail!!.listing.title)
        assertEquals("Alice", state.detail!!.sellerName)
    }

    @Test
    fun loadListing_nonOwnerShowsContactInfo() = runTest(testDispatcher) {
        val detail = MarketplaceListingDetail(
            listing = MarketplaceListing(
                id = "item_2",
                residentId = "res_2",
                communityId = "comm_1",
                title = "Microwave",
                description = "Working condition",
                category = MarketplaceCategory.APPLIANCES,
                price = 3000.0,
                contactPhone = "9876543211",
                status = ListingStatus.ACTIVE,
                imageUri = null,
                createdAt = 1000L,
                updatedAt = null
            ),
            sellerName = "Bob",
            flatNumber = "B-202",
            block = "Block B",
            contactPhone = "9876543211",
            isOwner = false
        )
        fakeRepository.listingDetailsResult = Result.success(detail)

        val savedStateHandle = SavedStateHandle(mapOf("listingId" to "item_2"))
        viewModel = ListingDetailViewModel(
            savedStateHandle = savedStateHandle,
            getListingDetailsUseCase = getListingDetailsUseCase,
            updateListingUseCase = updateListingUseCase,
            deleteListingUseCase = deleteListingUseCase
        )
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.detail)
        assertFalse(state.detail!!.isOwner)
        assertEquals("9876543211", state.detail!!.contactPhone)
    }

    @Test
    fun crossCommunityListing_cannotBeLoaded_showsError() = runTest(testDispatcher) {
        fakeRepository.listingDetailsResult = Result.failure(NoSuchElementException("Listing not found"))

        val savedStateHandle = SavedStateHandle(mapOf("listingId" to "comm2_item"))
        viewModel = ListingDetailViewModel(
            savedStateHandle = savedStateHandle,
            getListingDetailsUseCase = getListingDetailsUseCase,
            updateListingUseCase = updateListingUseCase,
            deleteListingUseCase = deleteListingUseCase
        )
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.detail)
        assertEquals("Listing not found", state.errorMessage)
    }

    @Test
    fun ownerAction_markAsSold_success() = runTest(testDispatcher) {
        val detail = MarketplaceListingDetail(
            listing = MarketplaceListing(
                id = "item_1",
                residentId = "res_1",
                communityId = "comm_1",
                title = "Item",
                description = "Desc",
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
        fakeRepository.listingDetailsResult = Result.success(detail)

        val savedStateHandle = SavedStateHandle(mapOf("listingId" to "item_1"))
        viewModel = ListingDetailViewModel(
            savedStateHandle = savedStateHandle,
            getListingDetailsUseCase = getListingDetailsUseCase,
            updateListingUseCase = updateListingUseCase,
            deleteListingUseCase = deleteListingUseCase
        )
        testScheduler.advanceUntilIdle()

        viewModel.onEvent(ListingDetailEvent.ConfirmMarkSold)
        testScheduler.advanceUntilIdle()

        assertEquals("Listing marked as Sold", viewModel.state.value.actionMessage)
    }

    @Test
    fun ownerAction_cancelListing_success() = runTest(testDispatcher) {
        val detail = MarketplaceListingDetail(
            listing = MarketplaceListing(
                id = "item_1",
                residentId = "res_1",
                communityId = "comm_1",
                title = "Item",
                description = "Desc",
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
        fakeRepository.listingDetailsResult = Result.success(detail)

        val savedStateHandle = SavedStateHandle(mapOf("listingId" to "item_1"))
        viewModel = ListingDetailViewModel(
            savedStateHandle = savedStateHandle,
            getListingDetailsUseCase = getListingDetailsUseCase,
            updateListingUseCase = updateListingUseCase,
            deleteListingUseCase = deleteListingUseCase
        )
        testScheduler.advanceUntilIdle()

        viewModel.onEvent(ListingDetailEvent.ConfirmCancelListing)
        testScheduler.advanceUntilIdle()

        assertEquals("Listing cancelled", viewModel.state.value.actionMessage)
    }

    @Test
    fun ownerAction_deleteListing_successSetsIsDeleted() = runTest(testDispatcher) {
        val detail = MarketplaceListingDetail(
            listing = MarketplaceListing(
                id = "item_1",
                residentId = "res_1",
                communityId = "comm_1",
                title = "Item",
                description = "Desc",
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
        fakeRepository.listingDetailsResult = Result.success(detail)

        val savedStateHandle = SavedStateHandle(mapOf("listingId" to "item_1"))
        viewModel = ListingDetailViewModel(
            savedStateHandle = savedStateHandle,
            getListingDetailsUseCase = getListingDetailsUseCase,
            updateListingUseCase = updateListingUseCase,
            deleteListingUseCase = deleteListingUseCase
        )
        testScheduler.advanceUntilIdle()

        viewModel.onEvent(ListingDetailEvent.ConfirmDelete)
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isDeleted)
    }
}
