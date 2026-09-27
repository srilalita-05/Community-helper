package com.communityos.marketplace

import com.communityos.marketplace.domain.GetCommunityListingsUseCase
import com.communityos.marketplace.domain.GetMyListingsUseCase
import com.communityos.marketplace.event.MarketplaceListEvent
import com.communityos.marketplace.model.ListingStatus
import com.communityos.marketplace.model.MarketplaceCategory
import com.communityos.marketplace.model.MarketplaceListing
import com.communityos.marketplace.state.MarketplaceTab
import com.communityos.marketplace.viewmodel.MarketplaceListViewModel
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
class MarketplaceListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeMarketplaceRepository
    private lateinit var getCommunityListingsUseCase: GetCommunityListingsUseCase
    private lateinit var getMyListingsUseCase: GetMyListingsUseCase
    private lateinit var viewModel: MarketplaceListViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeMarketplaceRepository()
        getCommunityListingsUseCase = GetCommunityListingsUseCase(fakeRepository)
        getMyListingsUseCase = GetMyListingsUseCase(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsPopulatedListingsSuccessfully() = runTest(testDispatcher) {
        val sampleListing = MarketplaceListing(
            id = "item_1",
            residentId = "res_1",
            communityId = "comm_1",
            title = "Dining Table",
            description = "Solid wood table",
            category = MarketplaceCategory.FURNITURE,
            price = 4500.0,
            contactPhone = "9876543210",
            status = ListingStatus.ACTIVE,
            imageUri = null,
            createdAt = 1000L,
            updatedAt = null
        )
        fakeRepository.communityListingsResult = Result.success(listOf(sampleListing))
        fakeRepository.myListingsResult = Result.success(emptyList())

        viewModel = MarketplaceListViewModel(getCommunityListingsUseCase, getMyListingsUseCase)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(1, state.communityListings.size)
        assertEquals(1, state.displayedListings.size)
        assertEquals("Dining Table", state.displayedListings[0].title)
    }

    @Test
    fun initialState_handlesEmptyState() = runTest(testDispatcher) {
        fakeRepository.communityListingsResult = Result.success(emptyList())
        fakeRepository.myListingsResult = Result.success(emptyList())

        viewModel = MarketplaceListViewModel(getCommunityListingsUseCase, getMyListingsUseCase)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertTrue(state.displayedListings.isEmpty())
    }

    @Test
    fun initialState_handlesError() = runTest(testDispatcher) {
        fakeRepository.communityListingsResult = Result.failure(RuntimeException("Database failed"))
        fakeRepository.myListingsResult = Result.failure(RuntimeException("Database failed"))

        viewModel = MarketplaceListViewModel(getCommunityListingsUseCase, getMyListingsUseCase)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertEquals("Database failed", state.errorMessage)
    }

    @Test
    fun selectCategory_filtersDisplayedListings() = runTest(testDispatcher) {
        val furniture = MarketplaceListing(
            id = "f_1",
            residentId = "res_1",
            communityId = "comm_1",
            title = "Chair",
            description = "Desk chair",
            category = MarketplaceCategory.FURNITURE,
            price = 500.0,
            contactPhone = "9876543210",
            status = ListingStatus.ACTIVE,
            imageUri = null,
            createdAt = 1000L,
            updatedAt = null
        )
        val electronics = MarketplaceListing(
            id = "e_1",
            residentId = "res_2",
            communityId = "comm_1",
            title = "Headphones",
            description = "Wireless",
            category = MarketplaceCategory.ELECTRONICS,
            price = 1500.0,
            contactPhone = "9876543211",
            status = ListingStatus.ACTIVE,
            imageUri = null,
            createdAt = 2000L,
            updatedAt = null
        )
        fakeRepository.communityListingsResult = Result.success(listOf(furniture, electronics))

        viewModel = MarketplaceListViewModel(getCommunityListingsUseCase, getMyListingsUseCase)
        testScheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.displayedListings.size)

        // Select FURNITURE
        viewModel.onEvent(MarketplaceListEvent.SelectCategory(MarketplaceCategory.FURNITURE))
        assertEquals(1, viewModel.state.value.displayedListings.size)
        assertEquals("Chair", viewModel.state.value.displayedListings[0].title)

        // Select ALL
        viewModel.onEvent(MarketplaceListEvent.SelectCategory(MarketplaceCategory.ALL))
        assertEquals(2, viewModel.state.value.displayedListings.size)
    }

    @Test
    fun selectTab_switchesBetweenCommunityAndMyListings() = runTest(testDispatcher) {
        val communityItem = MarketplaceListing(
            id = "c_1",
            residentId = "res_2",
            communityId = "comm_1",
            title = "Community Item",
            description = "Desc",
            category = MarketplaceCategory.OTHER,
            price = 200.0,
            contactPhone = "9876543211",
            status = ListingStatus.ACTIVE,
            imageUri = null,
            createdAt = 1000L,
            updatedAt = null
        )
        val myItem = MarketplaceListing(
            id = "m_1",
            residentId = "res_1",
            communityId = "comm_1",
            title = "My Item",
            description = "Desc",
            category = MarketplaceCategory.BOOKS,
            price = 300.0,
            contactPhone = "9876543210",
            status = ListingStatus.ACTIVE,
            imageUri = null,
            createdAt = 2000L,
            updatedAt = null
        )
        fakeRepository.communityListingsResult = Result.success(listOf(communityItem))
        fakeRepository.myListingsResult = Result.success(listOf(myItem))

        viewModel = MarketplaceListViewModel(getCommunityListingsUseCase, getMyListingsUseCase)
        testScheduler.advanceUntilIdle()

        // Default tab is ALL_LISTINGS
        assertEquals(MarketplaceTab.ALL_LISTINGS, viewModel.state.value.selectedTab)
        assertEquals("Community Item", viewModel.state.value.displayedListings[0].title)

        // Switch to MY_LISTINGS
        viewModel.onEvent(MarketplaceListEvent.SelectTab(MarketplaceTab.MY_LISTINGS))
        assertEquals(MarketplaceTab.MY_LISTINGS, viewModel.state.value.selectedTab)
        assertEquals("My Item", viewModel.state.value.displayedListings[0].title)
    }

    @Test
    fun refreshEvent_reloadsListings() = runTest(testDispatcher) {
        fakeRepository.communityListingsResult = Result.success(emptyList())
        viewModel = MarketplaceListViewModel(getCommunityListingsUseCase, getMyListingsUseCase)
        testScheduler.advanceUntilIdle()
        assertEquals(0, viewModel.state.value.displayedListings.size)

        fakeRepository.communityListingsResult = Result.success(
            listOf(
                MarketplaceListing(
                    id = "refreshed_1",
                    residentId = "res_1",
                    communityId = "comm_1",
                    title = "Refreshed Item",
                    description = "Desc",
                    category = MarketplaceCategory.OTHER,
                    price = 100.0,
                    contactPhone = "9876543210",
                    status = ListingStatus.ACTIVE,
                    imageUri = null,
                    createdAt = 1000L,
                    updatedAt = null
                )
            )
        )

        viewModel.onEvent(MarketplaceListEvent.Refresh)
        testScheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.displayedListings.size)
        assertEquals("Refreshed Item", viewModel.state.value.displayedListings[0].title)
    }
}
