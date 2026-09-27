package com.communityos.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.communityos.data.local.dao.MarketplaceDao
import com.communityos.data.local.entity.CommunityEntity
import com.communityos.data.local.entity.FlatEntity
import com.communityos.data.local.entity.MarketplaceListingEntity
import com.communityos.data.local.entity.UserEntity
import com.communityos.marketplace.model.ListingStatus
import com.communityos.marketplace.model.MarketplaceCategory
import com.communityos.models.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class MarketplaceDaoTest {

    private lateinit var database: CommunityDatabase
    private lateinit var marketplaceDao: MarketplaceDao

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CommunityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        marketplaceDao = database.marketplaceDao()

        // Insert prerequisite foreign keys (community, flat, user)
        database.communityDao().insertCommunity(
            CommunityEntity("comm_1", "Greenwood", "123 Main St", "City", 4)
        )
        database.communityDao().insertCommunity(
            CommunityEntity("comm_2", "Sunrise", "456 Oak St", "City", 2)
        )

        database.userDao().insertUser(
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
        database.userDao().insertUser(
            UserEntity(
                id = "res_2",
                phoneNumber = "9876543211",
                name = "Bob",
                email = "bob@example.com",
                role = UserRole.RESIDENT,
                communityId = "comm_2",
                flatId = null,
                isApproved = true,
                createdAt = 1000L
            )
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveListing_byListingIdAndCommunityId() = runTest {
        val listing = MarketplaceListingEntity(
            id = "item_1",
            residentId = "res_1",
            communityId = "comm_1",
            title = "Dining Table",
            description = "Solid wood 6-seater dining table",
            category = MarketplaceCategory.FURNITURE,
            price = 4500.0,
            contactPhone = "9876543210",
            status = ListingStatus.ACTIVE,
            imageUri = null,
            createdAt = 1000L,
            updatedAt = null
        )

        marketplaceDao.insertListing(listing)

        // Same community retrieves listing
        val retrieved = marketplaceDao.getListingByIdAndCommunity("item_1", "comm_1")
        assertNotNull(retrieved)
        assertEquals("Dining Table", retrieved?.title)
        assertEquals(4500.0, retrieved?.price ?: 0.0, 0.01)
        assertEquals(ListingStatus.ACTIVE, retrieved?.status)

        // Different community cannot retrieve listing
        val wrongComm = marketplaceDao.getListingByIdAndCommunity("item_1", "comm_2")
        assertNull(wrongComm)
    }

    @Test
    fun getActiveListingsForCommunity_onlyReturnsActiveListings() = runTest {
        val active1 = MarketplaceListingEntity(
            id = "item_1",
            residentId = "res_1",
            communityId = "comm_1",
            title = "Laptop",
            description = "Gaming laptop",
            category = MarketplaceCategory.ELECTRONICS,
            price = 55000.0,
            contactPhone = "9876543210",
            status = ListingStatus.ACTIVE,
            imageUri = null,
            createdAt = 1000L,
            updatedAt = null
        )
        val sold = MarketplaceListingEntity(
            id = "item_2",
            residentId = "res_1",
            communityId = "comm_1",
            title = "Coffee Machine",
            description = "Espresso maker",
            category = MarketplaceCategory.APPLIANCES,
            price = 3000.0,
            contactPhone = "9876543210",
            status = ListingStatus.SOLD,
            imageUri = null,
            createdAt = 2000L,
            updatedAt = 3000L
        )
        val cancelled = MarketplaceListingEntity(
            id = "item_3",
            residentId = "res_1",
            communityId = "comm_1",
            title = "Bicycle",
            description = "Mountain bike",
            category = MarketplaceCategory.VEHICLES,
            price = 8000.0,
            contactPhone = "9876543210",
            status = ListingStatus.CANCELLED,
            imageUri = null,
            createdAt = 1500L,
            updatedAt = 2500L
        )

        marketplaceDao.insertListing(active1)
        marketplaceDao.insertListing(sold)
        marketplaceDao.insertListing(cancelled)

        val activeListings = marketplaceDao.getActiveListingsForCommunity("comm_1")
        assertEquals(1, activeListings.size)
        assertEquals("item_1", activeListings[0].id)
    }

    @Test
    fun categoryFiltering_onlyReturnsMatchingCategory() = runTest {
        val furniture = MarketplaceListingEntity(
            id = "item_f",
            residentId = "res_1",
            communityId = "comm_1",
            title = "Sofa",
            description = "Leather sofa",
            category = MarketplaceCategory.FURNITURE,
            price = 12000.0,
            contactPhone = "9876543210",
            status = ListingStatus.ACTIVE,
            imageUri = null,
            createdAt = 1000L,
            updatedAt = null
        )
        val book = MarketplaceListingEntity(
            id = "item_b",
            residentId = "res_1",
            communityId = "comm_1",
            title = "Kotlin Book",
            description = "Programming guide",
            category = MarketplaceCategory.BOOKS,
            price = 500.0,
            contactPhone = "9876543210",
            status = ListingStatus.ACTIVE,
            imageUri = null,
            createdAt = 2000L,
            updatedAt = null
        )

        marketplaceDao.insertListing(furniture)
        marketplaceDao.insertListing(book)

        val books = marketplaceDao.getActiveListingsByCategoryForCommunity("comm_1", MarketplaceCategory.BOOKS)
        assertEquals(1, books.size)
        assertEquals("item_b", books[0].id)

        val furnitureListings = marketplaceDao.getActiveListingsByCategoryForCommunity("comm_1", MarketplaceCategory.FURNITURE)
        assertEquals(1, furnitureListings.size)
        assertEquals("item_f", furnitureListings[0].id)
    }

    @Test
    fun communityIsolation_listingsNeverLeakAcrossCommunities() = runTest {
        val comm1Listing = MarketplaceListingEntity(
            id = "item_c1",
            residentId = "res_1",
            communityId = "comm_1",
            title = "Comm 1 Item",
            description = "Description 1",
            category = MarketplaceCategory.OTHER,
            price = 100.0,
            contactPhone = "9876543210",
            status = ListingStatus.ACTIVE,
            imageUri = null,
            createdAt = 1000L,
            updatedAt = null
        )
        val comm2Listing = MarketplaceListingEntity(
            id = "item_c2",
            residentId = "res_2",
            communityId = "comm_2",
            title = "Comm 2 Item",
            description = "Description 2",
            category = MarketplaceCategory.SERVICES,
            price = 200.0,
            contactPhone = "9876543211",
            status = ListingStatus.ACTIVE,
            imageUri = null,
            createdAt = 2000L,
            updatedAt = null
        )

        marketplaceDao.insertListing(comm1Listing)
        marketplaceDao.insertListing(comm2Listing)

        val comm1List = marketplaceDao.getActiveListingsForCommunity("comm_1")
        assertEquals(1, comm1List.size)
        assertEquals("item_c1", comm1List[0].id)

        val comm2List = marketplaceDao.getActiveListingsForCommunity("comm_2")
        assertEquals(1, comm2List.size)
        assertEquals("item_c2", comm2List[0].id)
    }

    @Test
    fun getListingsForResident_returnsAllStatusesForResident() = runTest {
        val active = MarketplaceListingEntity(
            id = "item_1",
            residentId = "res_1",
            communityId = "comm_1",
            title = "Item 1",
            description = "Desc 1",
            category = MarketplaceCategory.FURNITURE,
            price = 500.0,
            contactPhone = "9876543210",
            status = ListingStatus.ACTIVE,
            imageUri = null,
            createdAt = 1000L,
            updatedAt = null
        )
        val sold = MarketplaceListingEntity(
            id = "item_2",
            residentId = "res_1",
            communityId = "comm_1",
            title = "Item 2",
            description = "Desc 2",
            category = MarketplaceCategory.ELECTRONICS,
            price = 1000.0,
            contactPhone = "9876543210",
            status = ListingStatus.SOLD,
            imageUri = null,
            createdAt = 2000L,
            updatedAt = 3000L
        )

        marketplaceDao.insertListing(active)
        marketplaceDao.insertListing(sold)

        val residentListings = marketplaceDao.getListingsForResident("res_1")
        assertEquals(2, residentListings.size)

        val otherResidentListings = marketplaceDao.getListingsForResident("res_2")
        assertTrue(otherResidentListings.isEmpty())
    }

    @Test
    fun updateListing_modifiesEntityCorrectly() = runTest {
        val listing = MarketplaceListingEntity(
            id = "item_1",
            residentId = "res_1",
            communityId = "comm_1",
            title = "Original Title",
            description = "Original Desc",
            category = MarketplaceCategory.OTHER,
            price = 100.0,
            contactPhone = "9876543210",
            status = ListingStatus.ACTIVE,
            imageUri = null,
            createdAt = 1000L,
            updatedAt = null
        )
        marketplaceDao.insertListing(listing)

        val updated = listing.copy(
            title = "Updated Title",
            price = 150.0,
            status = ListingStatus.SOLD,
            updatedAt = 2000L
        )
        marketplaceDao.updateListing(updated)

        val retrieved = marketplaceDao.getListingByIdAndCommunity("item_1", "comm_1")
        assertNotNull(retrieved)
        assertEquals("Updated Title", retrieved?.title)
        assertEquals(150.0, retrieved?.price ?: 0.0, 0.01)
        assertEquals(ListingStatus.SOLD, retrieved?.status)
        assertEquals(2000L, retrieved?.updatedAt)
    }

    @Test
    fun deleteListing_removesEntityFromDatabase() = runTest {
        val listing = MarketplaceListingEntity(
            id = "item_1",
            residentId = "res_1",
            communityId = "comm_1",
            title = "To Delete",
            description = "Desc",
            category = MarketplaceCategory.BOOKS,
            price = 50.0,
            contactPhone = "9876543210",
            status = ListingStatus.ACTIVE,
            imageUri = null,
            createdAt = 1000L,
            updatedAt = null
        )
        marketplaceDao.insertListing(listing)
        assertNotNull(marketplaceDao.getListingByIdAndCommunity("item_1", "comm_1"))

        marketplaceDao.deleteListing(listing)
        assertNull(marketplaceDao.getListingByIdAndCommunity("item_1", "comm_1"))
    }

    @Test
    fun observeActiveListingsForCommunity_emitsRealtimeUpdates() = runTest {
        val listing = MarketplaceListingEntity(
            id = "item_flow",
            residentId = "res_1",
            communityId = "comm_1",
            title = "Flow Item",
            description = "Desc",
            category = MarketplaceCategory.ELECTRONICS,
            price = 999.0,
            contactPhone = "9876543210",
            status = ListingStatus.ACTIVE,
            imageUri = null,
            createdAt = 1000L,
            updatedAt = null
        )
        marketplaceDao.insertListing(listing)

        val emissions = marketplaceDao.observeActiveListingsForCommunity("comm_1").first()
        assertEquals(1, emissions.size)
        assertEquals("item_flow", emissions[0].id)
    }
}
