package com.communityos.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.communityos.data.local.CommunityDatabase
import com.communityos.data.local.entity.CommunityEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class CommunityDaoTest {

    private lateinit var database: CommunityDatabase
    private lateinit var communityDao: CommunityDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CommunityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        communityDao = database.communityDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertCommunity_and_getCommunityById_returns_matching_entity() = runTest {
        val community = CommunityEntity(
            id = "comm_1",
            name = "Orchard Heights Apartments",
            address = "42 Greenfield Blvd",
            city = "Bengaluru",
            totalBlocks = 4
        )

        communityDao.insertCommunity(community)
        val loaded = communityDao.getCommunityById("comm_1")

        assertNotNull(loaded)
        assertEquals("Orchard Heights Apartments", loaded?.name)
        assertEquals("Bengaluru", loaded?.city)
        assertEquals(4, loaded?.totalBlocks)
    }
}
