package com.communityos.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.communityos.data.local.dao.CommunityDao
import com.communityos.data.local.dao.FlatDao
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
    private lateinit var initializer: DatabaseInitializer

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CommunityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        communityDao = database.communityDao()
        flatDao = database.flatDao()
        initializer = DatabaseInitializer(communityDao, flatDao)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun seedDemoDataIfEmpty_seeds_when_empty_and_is_idempotent() = runTest {
        assertEquals(0, communityDao.getCount())
        assertEquals(0, flatDao.getCount())

        // First run seeds data
        initializer.seedDemoDataIfEmpty()
        val initialCommunityCount = communityDao.getCount()
        val initialFlatCount = flatDao.getCount()

        assertTrue(initialCommunityCount > 0)
        assertTrue(initialFlatCount > 0)

        // Second run must be idempotent (no duplicates inserted)
        initializer.seedDemoDataIfEmpty()
        assertEquals(initialCommunityCount, communityDao.getCount())
        assertEquals(initialFlatCount, flatDao.getCount())
    }
}
