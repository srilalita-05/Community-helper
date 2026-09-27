package com.communityos.marketplace

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.communityos.data.local.CommunityDatabase
import com.communityos.data.local.dao.CommunityDao
import com.communityos.data.local.dao.FlatDao
import com.communityos.data.local.dao.MarketplaceDao
import com.communityos.data.local.dao.UserDao
import com.communityos.data.local.entity.CommunityEntity
import com.communityos.data.local.entity.FlatEntity
import com.communityos.data.local.entity.UserEntity
import com.communityos.data.local.session.SessionManager
import com.communityos.marketplace.model.ListingStatus
import com.communityos.marketplace.model.MarketplaceCategory
import com.communityos.marketplace.repository.MarketplaceRepository
import com.communityos.marketplace.repository.MarketplaceRepositoryImpl
import com.communityos.models.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class MarketplaceRepositoryTest {

    private lateinit var database: CommunityDatabase
    private lateinit var marketplaceDao: MarketplaceDao
    private lateinit var userDao: UserDao
    private lateinit var flatDao: FlatDao
    private lateinit var communityDao: CommunityDao
    private lateinit var sessionManager: SessionManager
    private lateinit var repository: MarketplaceRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CommunityDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        marketplaceDao = database.marketplaceDao()
        userDao = database.userDao()
        flatDao = database.flatDao()
        communityDao = database.communityDao()

        val testFile = context.preferencesDataStoreFile("test_marketplace_repo_${System.nanoTime()}")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { testFile }
        )
        sessionManager = SessionManager(testDataStore)

        repository = MarketplaceRepositoryImpl(
            sessionManager = sessionManager,
            userDao = userDao,
            communityDao = communityDao,
            flatDao = flatDao,
            marketplaceDao = marketplaceDao
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    private suspend fun seedUsersAndCommunities() {
        // Communities
        communityDao.insertCommunity(CommunityEntity("comm_1", "Greenwood", "123 Main St", "City", 4))
        communityDao.insertCommunity(CommunityEntity("comm_2", "Sunrise", "456 Oak St", "City", 2))

        // Flats
        flatDao.insertFlat(FlatEntity("flat_1", "comm_1", "Block A", "A-101", 1))
        flatDao.insertFlat(FlatEntity("flat_2", "comm_1", "Block B", "B-202", 2))
        flatDao.insertFlat(FlatEntity("flat_3", "comm_2", "Block C", "C-303", 3))

        // Resident 1 in Community 1
        userDao.insertUser(
            UserEntity("res_1", "9876543210", "Alice", "alice@example.com", UserRole.RESIDENT, "comm_1", "flat_1")
        )
        // Resident 2 in Community 1
        userDao.insertUser(
            UserEntity("res_2", "9876543211", "Bob", "bob@example.com", UserRole.RESIDENT, "comm_1", "flat_2")
        )
        // Resident 3 in Community 2
        userDao.insertUser(
            UserEntity("res_3", "9876543212", "Charlie", "charlie@example.com", UserRole.RESIDENT, "comm_2", "flat_3")
        )
        // Admin
        userDao.insertUser(
            UserEntity("admin_1", "9876543213", "Admin User", "admin@example.com", UserRole.ADMIN, "comm_1", null)
        )
    }

    @Test
    fun identityDerivedFromSession_unauthenticatedFails() = runTest {
        seedUsersAndCommunities()
        // No session saved
        val result = repository.createListing(
            title = "Chair",
            description = "Comfortable office chair",
            category = MarketplaceCategory.FURNITURE,
            price = 1500.0,
            contactPhone = "9876543210"
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun nonResidentRole_cannotCreateListing() = runTest {
        seedUsersAndCommunities()
        sessionManager.saveSession("admin_1", UserRole.ADMIN)

        val result = repository.createListing(
            title = "Chair",
            description = "Desk chair",
            category = MarketplaceCategory.FURNITURE,
            price = 1500.0,
            contactPhone = "9876543213"
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun createListing_successWithDerivedIdentity() = runTest {
        seedUsersAndCommunities()
        sessionManager.saveSession("res_1", UserRole.RESIDENT)

        val result = repository.createListing(
            title = "Bicycle",
            description = "Mountain bike in good shape",
            category = MarketplaceCategory.VEHICLES,
            price = 5000.0,
            contactPhone = "9876543210"
        )
        assertTrue(result.isSuccess)
        val listing = result.getOrThrow()
        assertEquals("Bicycle", listing.title)
        assertEquals("res_1", listing.residentId)
        assertEquals("comm_1", listing.communityId)
        assertEquals(ListingStatus.ACTIVE, listing.status)
        assertNull(listing.imageUri)
    }

    @Test
    fun communityIsolation_residentCannotAccessOtherCommunityListings() = runTest {
        seedUsersAndCommunities()

        // Resident 3 in comm_2 creates listing
        sessionManager.saveSession("res_3", UserRole.RESIDENT)
        val createRes = repository.createListing(
            title = "Comm 2 Lamp",
            description = "Desk lamp",
            category = MarketplaceCategory.ELECTRONICS,
            price = 800.0,
            contactPhone = "9876543212"
        )
        val listingId = createRes.getOrThrow().id

        // Resident 1 in comm_1 tries to get details for listing from comm_2
        sessionManager.saveSession("res_1", UserRole.RESIDENT)
        val detailRes = repository.getListingDetails(listingId)
        assertTrue(detailRes.isFailure)
        assertTrue(detailRes.exceptionOrNull() is NoSuchElementException)

        // Resident 1's community browse does not include listing from comm_2
        val browseRes = repository.getCommunityListings(null)
        assertTrue(browseRes.isSuccess)
        assertTrue(browseRes.getOrThrow().none { it.id == listingId })
    }

    @Test
    fun ownershipEnforced_nonOwnerCannotEditListing() = runTest {
        seedUsersAndCommunities()

        // Resident 1 creates listing
        sessionManager.saveSession("res_1", UserRole.RESIDENT)
        val listing = repository.createListing(
            title = "Study Table",
            description = "Wooden table",
            category = MarketplaceCategory.FURNITURE,
            price = 2500.0,
            contactPhone = "9876543210"
        ).getOrThrow()

        // Resident 2 (same community, but not owner) attempts to edit
        sessionManager.saveSession("res_2", UserRole.RESIDENT)
        val editRes = repository.updateListing(
            listingId = listing.id,
            title = "Hacked Title",
            description = "Hacked Desc",
            category = MarketplaceCategory.FURNITURE,
            price = 100.0,
            contactPhone = "9876543211",
            status = ListingStatus.ACTIVE
        )
        assertTrue(editRes.isFailure)
        assertTrue(editRes.exceptionOrNull() is SecurityException)
    }

    @Test
    fun ownershipEnforced_nonOwnerCannotDeleteListing() = runTest {
        seedUsersAndCommunities()

        // Resident 1 creates listing
        sessionManager.saveSession("res_1", UserRole.RESIDENT)
        val listing = repository.createListing(
            title = "Monitor",
            description = "24 inch monitor",
            category = MarketplaceCategory.ELECTRONICS,
            price = 6000.0,
            contactPhone = "9876543210"
        ).getOrThrow()

        // Resident 2 attempts to delete
        sessionManager.saveSession("res_2", UserRole.RESIDENT)
        val deleteRes = repository.deleteListing(listing.id)
        assertTrue(deleteRes.isFailure)
        assertTrue(deleteRes.exceptionOrNull() is SecurityException)

        // Verify listing still exists
        sessionManager.saveSession("res_1", UserRole.RESIDENT)
        val details = repository.getListingDetails(listing.id)
        assertTrue(details.isSuccess)
    }

    @Test
    fun ownershipEnforced_nonOwnerCannotMarkSoldOrCancel() = runTest {
        seedUsersAndCommunities()

        sessionManager.saveSession("res_1", UserRole.RESIDENT)
        val listing = repository.createListing(
            title = "Heater",
            description = "Room heater",
            category = MarketplaceCategory.APPLIANCES,
            price = 1200.0,
            contactPhone = "9876543210"
        ).getOrThrow()

        // Resident 2 attempts to mark sold
        sessionManager.saveSession("res_2", UserRole.RESIDENT)
        val soldRes = repository.markListingAsSold(listing.id)
        assertTrue(soldRes.isFailure)
        assertTrue(soldRes.exceptionOrNull() is SecurityException)

        // Resident 2 attempts to cancel
        val cancelRes = repository.cancelListing(listing.id)
        assertTrue(cancelRes.isFailure)
        assertTrue(cancelRes.exceptionOrNull() is SecurityException)
    }

    @Test
    fun stateTransitions_validTransitionsSucceed() = runTest {
        seedUsersAndCommunities()
        sessionManager.saveSession("res_1", UserRole.RESIDENT)

        // ACTIVE -> SOLD
        val listing1 = repository.createListing(
            title = "Microwave",
            description = "Oven",
            category = MarketplaceCategory.APPLIANCES,
            price = 3500.0,
            contactPhone = "9876543210"
        ).getOrThrow()

        val markSoldRes = repository.markListingAsSold(listing1.id)
        assertTrue(markSoldRes.isSuccess)
        assertEquals(ListingStatus.SOLD, markSoldRes.getOrThrow().status)

        // ACTIVE -> CANCELLED
        val listing2 = repository.createListing(
            title = "Blender",
            description = "Smoothie maker",
            category = MarketplaceCategory.APPLIANCES,
            price = 1500.0,
            contactPhone = "9876543210"
        ).getOrThrow()

        val cancelRes = repository.cancelListing(listing2.id)
        assertTrue(cancelRes.isSuccess)
        assertEquals(ListingStatus.CANCELLED, cancelRes.getOrThrow().status)
    }

    @Test
    fun stateTransitions_invalidTransitionsFail() = runTest {
        seedUsersAndCommunities()
        sessionManager.saveSession("res_1", UserRole.RESIDENT)

        val listing = repository.createListing(
            title = "Sofa",
            description = "Couch",
            category = MarketplaceCategory.FURNITURE,
            price = 8000.0,
            contactPhone = "9876543210"
        ).getOrThrow()

        // Transition to SOLD
        repository.markListingAsSold(listing.id)

        // Attempt SOLD -> ACTIVE (terminal state)
        val revertRes = repository.updateListing(
            listingId = listing.id,
            title = "Sofa",
            description = "Couch",
            category = MarketplaceCategory.FURNITURE,
            price = 8000.0,
            contactPhone = "9876543210",
            status = ListingStatus.ACTIVE
        )
        assertTrue(revertRes.isFailure)
        assertTrue(revertRes.exceptionOrNull() is IllegalStateException)

        // Attempt to edit fields on SOLD listing (even keeping SOLD status)
        val editSoldRes = repository.updateListing(
            listingId = listing.id,
            title = "Modified Sofa",
            description = "Modified Couch",
            category = MarketplaceCategory.FURNITURE,
            price = 7000.0,
            contactPhone = "9876543210",
            status = ListingStatus.SOLD
        )
        assertTrue(editSoldRes.isFailure)
        assertTrue(editSoldRes.exceptionOrNull() is IllegalStateException)

        // Attempt SOLD -> CANCELLED
        val cancelRes = repository.cancelListing(listing.id)
        assertTrue(cancelRes.isFailure)
        assertTrue(cancelRes.exceptionOrNull() is IllegalStateException)

        // Create a second listing to test CANCELLED terminal state
        val listing2 = repository.createListing(
            title = "Bed",
            description = "King size",
            category = MarketplaceCategory.FURNITURE,
            price = 15000.0,
            contactPhone = "9876543210"
        ).getOrThrow()

        // Transition to CANCELLED
        repository.cancelListing(listing2.id)

        // Attempt CANCELLED -> ACTIVE
        val reactivateRes = repository.updateListing(
            listingId = listing2.id,
            title = "Bed",
            description = "King size",
            category = MarketplaceCategory.FURNITURE,
            price = 15000.0,
            contactPhone = "9876543210",
            status = ListingStatus.ACTIVE
        )
        assertTrue(reactivateRes.isFailure)
        assertTrue(reactivateRes.exceptionOrNull() is IllegalStateException)

        // Attempt CANCELLED -> SOLD
        val markCancelledAsSoldRes = repository.markListingAsSold(listing2.id)
        assertTrue(markCancelledAsSoldRes.isFailure)
        assertTrue(markCancelledAsSoldRes.exceptionOrNull() is IllegalStateException)

        // Attempt to edit fields on CANCELLED listing
        val editCancelledRes = repository.updateListing(
            listingId = listing2.id,
            title = "New Bed Title",
            description = "New Desc",
            category = MarketplaceCategory.FURNITURE,
            price = 12000.0,
            contactPhone = "9876543210",
            status = ListingStatus.CANCELLED
        )
        assertTrue(editCancelledRes.isFailure)
        assertTrue(editCancelledRes.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun ownerCanDeleteListing_removesPermanently() = runTest {
        seedUsersAndCommunities()
        sessionManager.saveSession("res_1", UserRole.RESIDENT)

        val listing = repository.createListing(
            title = "Books Set",
            description = "Novels",
            category = MarketplaceCategory.BOOKS,
            price = 400.0,
            contactPhone = "9876543210"
        ).getOrThrow()

        val deleteRes = repository.deleteListing(listing.id)
        assertTrue(deleteRes.isSuccess)

        val detailRes = repository.getListingDetails(listing.id)
        assertTrue(detailRes.isFailure)
    }

    @Test
    fun getListingDetails_resolvesSellerAndFlatInfo() = runTest {
        seedUsersAndCommunities()
        sessionManager.saveSession("res_1", UserRole.RESIDENT)

        val listing = repository.createListing(
            title = "Iron Box",
            description = "Dry iron",
            category = MarketplaceCategory.APPLIANCES,
            price = 300.0,
            contactPhone = "9876543210"
        ).getOrThrow()

        // Viewed by Resident 2
        sessionManager.saveSession("res_2", UserRole.RESIDENT)
        val details = repository.getListingDetails(listing.id).getOrThrow()

        assertEquals("Alice", details.sellerName)
        assertEquals("A-101", details.flatNumber)
        assertEquals("Block A", details.block)
        assertEquals("9876543210", details.contactPhone)
        assertFalse(details.isOwner)

        // Viewed by Resident 1 (owner)
        sessionManager.saveSession("res_1", UserRole.RESIDENT)
        val ownerDetails = repository.getListingDetails(listing.id).getOrThrow()
        assertTrue(ownerDetails.isOwner)
    }
}
