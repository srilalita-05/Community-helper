package com.communityos.home

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.communityos.data.local.CommunityDatabase
import com.communityos.data.local.dao.CommunityDao
import com.communityos.data.local.dao.FlatDao
import com.communityos.data.local.dao.UserDao
import com.communityos.data.local.entity.CommunityEntity
import com.communityos.data.local.entity.FlatEntity
import com.communityos.data.local.entity.UserEntity
import com.communityos.data.local.session.SessionManager
import com.communityos.home.repository.HomeRepository
import com.communityos.home.repository.HomeRepositoryImpl
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
class HomeRepositoryTest {

    private lateinit var database: CommunityDatabase
    private lateinit var userDao: UserDao
    private lateinit var communityDao: CommunityDao
    private lateinit var flatDao: FlatDao
    private lateinit var sessionManager: SessionManager
    private lateinit var homeRepository: HomeRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CommunityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = database.userDao()
        communityDao = database.communityDao()
        flatDao = database.flatDao()

        val testFile = context.preferencesDataStoreFile("test_home_repo_${System.nanoTime()}")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { testFile }
        )
        sessionManager = SessionManager(testDataStore)

        homeRepository = HomeRepositoryImpl(
            userDao = userDao,
            communityDao = communityDao,
            flatDao = flatDao,
            sessionManager = sessionManager
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun getDashboardSummary_withActiveSessionAndUser_resolvesCommunityAndFlat() = runTest {
        val community = CommunityEntity(
            id = "comm_orchard",
            name = "Orchard Heights Apartments",
            address = "42 Greenfield Boulevard",
            city = "Bengaluru",
            totalBlocks = 4
        )
        communityDao.insertCommunity(community)

        val flat = FlatEntity(
            id = "flat_b304",
            communityId = "comm_orchard",
            block = "Block B",
            flatNumber = "B-304",
            floor = 3
        )
        flatDao.insertFlat(flat)

        val user = UserEntity(
            id = "user_resident_1",
            phoneNumber = "9876543210",
            name = "David Miller",
            role = UserRole.RESIDENT,
            communityId = "comm_orchard",
            flatId = "flat_b304"
        )
        userDao.insertUser(user)
        sessionManager.saveSession("user_resident_1", UserRole.RESIDENT)

        val result = homeRepository.getDashboardSummary()
        assertTrue(result.isSuccess)

        val summary = result.getOrNull()
        assertNotNull(summary)
        assertEquals("Orchard Heights Apartments", summary?.communityName)
        assertEquals("B-304", summary?.flatNo)
        assertEquals("Block B", summary?.blockNo)
    }

    @Test
    fun getDashboardSummary_withoutSession_returnsFailureSafely() = runTest {
        val result = homeRepository.getDashboardSummary()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun getDashboardSummary_withMissingUser_returnsFailureSafely() = runTest {
        sessionManager.saveSession("ghost_user", UserRole.RESIDENT)

        val result = homeRepository.getDashboardSummary()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun getDashboardSummary_withNullCommunityAndFlat_handlesSafely() = runTest {
        val user = UserEntity(
            id = "user_unlinked",
            phoneNumber = "9123456780",
            name = "New User",
            role = UserRole.RESIDENT,
            communityId = null,
            flatId = null
        )
        userDao.insertUser(user)
        sessionManager.saveSession("user_unlinked", UserRole.RESIDENT)

        val result = homeRepository.getDashboardSummary()
        assertTrue(result.isSuccess)

        val summary = result.getOrNull()
        assertNotNull(summary)
        assertEquals("Welcome Home", summary?.communityName)
        assertEquals("", summary?.flatNo)
        assertEquals("", summary?.blockNo)
    }
}
