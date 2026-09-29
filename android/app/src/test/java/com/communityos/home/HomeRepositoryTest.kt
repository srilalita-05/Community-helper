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
    private lateinit var visitorDao: com.communityos.data.local.dao.VisitorDao
    private lateinit var maintenanceDao: com.communityos.data.local.dao.MaintenanceDao
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
        visitorDao = database.visitorDao()
        maintenanceDao = database.maintenanceDao()

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
            visitorDao = visitorDao,
            maintenanceDao = maintenanceDao,
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

    @Test
    fun getDashboardSummary_returnsDynamicMaintenanceDuesFromRoom() = runTest {
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
        val otherFlat = FlatEntity(
            id = "flat_a101",
            communityId = "comm_orchard",
            block = "Block A",
            flatNumber = "A-101",
            floor = 1
        )
        flatDao.insertFlat(flat)
        flatDao.insertFlat(otherFlat)

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

        val now = System.currentTimeMillis()
        val bills = listOf(
            com.communityos.data.local.entity.MaintenanceBillEntity(
                id = "b1",
                flatId = "flat_b304",
                communityId = "comm_orchard",
                title = "Maintenance",
                period = "October",
                amount = 2500.0,
                dueDate = now + 100000,
                status = com.communityos.maintenance.model.BillStatus.UNPAID,
                createdAt = now
            ),
            com.communityos.data.local.entity.MaintenanceBillEntity(
                id = "b2",
                flatId = "flat_b304",
                communityId = "comm_orchard",
                title = "Levy",
                period = "September",
                amount = 1200.0,
                dueDate = now - 100000,
                status = com.communityos.maintenance.model.BillStatus.OVERDUE,
                createdAt = now
            ),
            com.communityos.data.local.entity.MaintenanceBillEntity(
                id = "b3",
                flatId = "flat_b304",
                communityId = "comm_orchard",
                title = "August Maintenance",
                period = "August",
                amount = 800.0,
                dueDate = now - 200000,
                status = com.communityos.maintenance.model.BillStatus.PAID,
                createdAt = now
            ),
            com.communityos.data.local.entity.MaintenanceBillEntity(
                id = "b_other",
                flatId = "flat_a101",
                communityId = "comm_orchard",
                title = "Other Flat Maintenance",
                period = "October",
                amount = 9999.0,
                dueDate = now + 100000,
                status = com.communityos.maintenance.model.BillStatus.UNPAID,
                createdAt = now
            )
        )
        maintenanceDao.insertBills(bills)

        val result = homeRepository.getDashboardSummary()
        assertTrue(result.isSuccess)
        val summary = result.getOrNull()
        assertNotNull(summary)
        assertEquals(3700.0, summary?.outstandingDues ?: 0.0, 0.001)
    }
}
