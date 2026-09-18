package com.communityos.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.communityos.data.local.dao.CommunityDao
import com.communityos.data.local.dao.FlatDao
import com.communityos.data.local.dao.UserDao
import com.communityos.data.local.entity.UserEntity
import com.communityos.models.UserRole
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class DatabaseInitializerTest {

    private lateinit var database: CommunityDatabase
    private lateinit var communityDao: CommunityDao
    private lateinit var flatDao: FlatDao
    private lateinit var userDao: UserDao
    private lateinit var context: Context
    private lateinit var initializer: DatabaseInitializer

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CommunityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        communityDao = database.communityDao()
        flatDao = database.flatDao()
        userDao = database.userDao()
        initializer = DatabaseInitializer(context, communityDao, flatDao)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun seedDemoDataIfEmpty_loads_from_asset_and_populates_communities_and_flats() = runTest {
        assertEquals(0, communityDao.getCount())
        assertEquals(0, flatDao.getCount())

        initializer.seedDemoDataIfEmpty()

        val communities = communityDao.getAllCommunities()
        assertEquals(2, communities.size)

        val orchard = communities.find { it.id == "community_orchard_heights" }
        assertNotNull(orchard)
        assertEquals("Orchard Heights Apartments", orchard?.name)
        assertEquals("Bengaluru", orchard?.city)
        assertEquals(4, orchard?.totalBlocks)

        val palm = communities.find { it.id == "community_palm_meadows" }
        assertNotNull(palm)
        assertEquals("Palm Meadows Villa", palm?.name)
        assertEquals("Hyderabad", palm?.city)
        assertEquals(2, palm?.totalBlocks)

        val flats = flatDao.getCount()
        assertTrue(flats > 0)
    }

    @Test
    fun seedDemoDataIfEmpty_preserves_community_and_block_associations() = runTest {
        initializer.seedDemoDataIfEmpty()

        val orchardFlats = flatDao.getFlatsForCommunity("community_orchard_heights")
        assertTrue(orchardFlats.isNotEmpty())
        orchardFlats.forEach { flat ->
            assertEquals("community_orchard_heights", flat.communityId)
        }

        val flatA101 = orchardFlats.find { it.flatNumber == "A-101" }
        assertNotNull(flatA101)
        assertEquals("Block A", flatA101?.block)
        assertEquals(1, flatA101?.floor)

        val flatB304 = orchardFlats.find { it.flatNumber == "B-304" }
        assertNotNull(flatB304)
        assertEquals("Block B", flatB304?.block)
        assertEquals(3, flatB304?.floor)

        val palmFlats = flatDao.getFlatsForCommunity("community_palm_meadows")
        assertTrue(palmFlats.isNotEmpty())
        palmFlats.forEach { flat ->
            assertEquals("community_palm_meadows", flat.communityId)
        }

        val flatV101 = palmFlats.find { it.flatNumber == "V-101" }
        assertNotNull(flatV101)
        assertEquals("Villa 1", flatV101?.block)
    }

    @Test
    fun seedDemoDataIfEmpty_is_idempotent_no_duplicates_on_repeated_runs() = runTest {
        initializer.seedDemoDataIfEmpty()
        val initialCommunityCount = communityDao.getCount()
        val initialFlatCount = flatDao.getCount()

        assertTrue(initialCommunityCount > 0)
        assertTrue(initialFlatCount > 0)

        // Second run must be completely idempotent
        initializer.seedDemoDataIfEmpty()
        assertEquals(initialCommunityCount, communityDao.getCount())
        assertEquals(initialFlatCount, flatDao.getCount())
    }

    @Test
    fun seedDemoDataIfEmpty_does_not_wipe_existing_users_or_data() = runTest {
        val existingUser = UserEntity(
            id = "user_existing_123",
            phoneNumber = "+919876543210",
            name = "Existing User",
            email = "existing@example.com",
            role = UserRole.RESIDENT,
            communityId = "community_orchard_heights",
            flatId = "flat_a101",
            isApproved = true
        )
        userDao.insertUser(existingUser)

        initializer.seedDemoDataIfEmpty()

        val retrievedUser = userDao.getUserById("user_existing_123")
        assertNotNull(retrievedUser)
        assertEquals("+919876543210", retrievedUser?.phoneNumber)
        assertEquals("Existing User", retrievedUser?.name)
    }

    @Test
    fun community_scoped_flat_lookup_succeeds_for_valid_flat_and_rejects_cross_community_flat() = runTest {
        initializer.seedDemoDataIfEmpty()

        // Valid inside Orchard Heights
        val validOrchard = flatDao.getFlatByNumber("community_orchard_heights", "A-101")
        assertNotNull(validOrchard)
        assertEquals("A-101", validOrchard?.flatNumber)
        assertEquals("community_orchard_heights", validOrchard?.communityId)

        // Cross-community rejection: A-101 inside Palm Meadows must NOT exist
        val crossCommunityReject1 = flatDao.getFlatByNumber("community_palm_meadows", "A-101")
        assertNull(crossCommunityReject1)

        // Valid inside Palm Meadows
        val validPalm = flatDao.getFlatByNumber("community_palm_meadows", "V-101")
        assertNotNull(validPalm)
        assertEquals("V-101", validPalm?.flatNumber)
        assertEquals("community_palm_meadows", validPalm?.communityId)

        // Cross-community rejection: V-101 inside Orchard Heights must NOT exist
        val crossCommunityReject2 = flatDao.getFlatByNumber("community_orchard_heights", "V-101")
        assertNull(crossCommunityReject2)
    }

    @Test
    fun seedFromJson_correctly_parses_custom_json_and_derives_totalBlocks() = runTest {
        val customJson = """
            {
              "communities": [
                {
                  "id": "comm_custom",
                  "name": "Custom Residency",
                  "address": "123 Test Street",
                  "city": "Chennai",
                  "blocks": [
                    {
                      "id": "blk_tower1",
                      "name": "Tower 1",
                      "flats": [
                        {
                          "id": "flt_t1_101",
                          "number": "T1-101",
                          "floor": 1
                        },
                        {
                          "id": "flt_t1_102",
                          "number": "T1-102",
                          "floor": 1
                        }
                      ]
                    },
                    {
                      "id": "blk_tower2",
                      "name": "Tower 2",
                      "flats": [
                        {
                          "id": "flt_t2_201",
                          "number": "T2-201",
                          "floor": 2
                        }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        initializer.seedFromJson(customJson)

        val community = communityDao.getCommunityById("comm_custom")
        assertNotNull(community)
        assertEquals("Custom Residency", community?.name)
        assertEquals("Chennai", community?.city)
        assertEquals(2, community?.totalBlocks) // Derived from blocks.length

        val flats = flatDao.getFlatsForCommunity("comm_custom")
        assertEquals(3, flats.size)

        val t1101 = flatDao.getFlatByNumber("comm_custom", "T1-101")
        assertNotNull(t1101)
        assertEquals("flt_t1_101", t1101?.id)
        assertEquals("Tower 1", t1101?.block)
        assertEquals(1, t1101?.floor)
    }
}
