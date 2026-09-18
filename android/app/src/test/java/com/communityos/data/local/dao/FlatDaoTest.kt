package com.communityos.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.communityos.data.local.CommunityDatabase
import com.communityos.data.local.entity.FlatEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class FlatDaoTest {

    private lateinit var database: CommunityDatabase
    private lateinit var flatDao: FlatDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CommunityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        flatDao = database.flatDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertFlat_and_getFlatById_returns_matching_entity() = runTest {
        val flat = FlatEntity(
            id = "flat_1",
            communityId = "comm_1",
            block = "Block B",
            flatNumber = "B-304",
            floor = 3
        )

        flatDao.insertFlat(flat)
        val loaded = flatDao.getFlatById("flat_1")

        assertNotNull(loaded)
        assertEquals("Block B", loaded?.block)
        assertEquals("B-304", loaded?.flatNumber)
        assertEquals(3, loaded?.floor)
    }

    @Test
    fun getFlatsForCommunity_returns_correct_flats() = runTest {
        val flats = listOf(
            FlatEntity("flat_1", "comm_1", "Block A", "A-101", 1),
            FlatEntity("flat_2", "comm_1", "Block B", "B-304", 3),
            FlatEntity("flat_3", "comm_2", "Villa 1", "V-101", 1)
        )

        flatDao.insertFlats(flats)
        val comm1Flats = flatDao.getFlatsForCommunity("comm_1")

        assertEquals(2, comm1Flats.size)
        assertTrue(comm1Flats.any { it.flatNumber == "A-101" })
        assertTrue(comm1Flats.any { it.flatNumber == "B-304" })
        assertFalse(comm1Flats.any { it.flatNumber == "V-101" })
    }
}
