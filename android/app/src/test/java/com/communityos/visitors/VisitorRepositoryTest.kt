package com.communityos.visitors

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.communityos.data.local.CommunityDatabase
import com.communityos.data.local.dao.CommunityDao
import com.communityos.data.local.dao.FlatDao
import com.communityos.data.local.dao.UserDao
import com.communityos.data.local.dao.VisitorDao
import com.communityos.data.local.entity.CommunityEntity
import com.communityos.data.local.entity.FlatEntity
import com.communityos.data.local.entity.UserEntity
import com.communityos.data.local.session.SessionManager
import com.communityos.models.UserRole
import com.communityos.visitors.model.VisitorStatus
import com.communityos.visitors.repository.VisitorRepository
import com.communityos.visitors.repository.VisitorRepositoryImpl
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
class VisitorRepositoryTest {

    private lateinit var database: CommunityDatabase
    private lateinit var visitorDao: VisitorDao
    private lateinit var userDao: UserDao
    private lateinit var flatDao: FlatDao
    private lateinit var communityDao: CommunityDao
    private lateinit var sessionManager: SessionManager
    private lateinit var repository: VisitorRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CommunityDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        visitorDao = database.visitorDao()
        userDao = database.userDao()
        flatDao = database.flatDao()
        communityDao = database.communityDao()

        val testFile = context.preferencesDataStoreFile("test_visitor_repo_${System.nanoTime()}")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { testFile }
        )
        sessionManager = SessionManager(testDataStore)

        repository = VisitorRepositoryImpl(
            visitorDao = visitorDao,
            userDao = userDao,
            flatDao = flatDao,
            communityDao = communityDao,
            sessionManager = sessionManager
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    private suspend fun seedCommunitiesAndUsers() {
        // Community 1
        communityDao.insertCommunity(
            CommunityEntity("comm_1", "Orchard Heights Apartments", "42 Greenfield Blvd", "Bengaluru", 4)
        )
        // Community 2
        communityDao.insertCommunity(
            CommunityEntity("comm_2", "Palm Springs", "10 Palm Avenue", "Bengaluru", 2)
        )

        // Flats
        flatDao.insertFlat(FlatEntity("flat_1", "comm_1", "Block B", "B-304", 3))
        flatDao.insertFlat(FlatEntity("flat_2", "comm_1", "Block A", "A-101", 1))
        flatDao.insertFlat(FlatEntity("flat_3", "comm_2", "Tower 1", "T1-502", 5))

        // Users
        // Resident 1 in Community 1, Flat 1
        userDao.insertUser(
            UserEntity("res_1", "9876543210", "David Miller", "david@example.com", UserRole.RESIDENT, "comm_1", "flat_1")
        )
        // Resident 2 in Community 1, Flat 2
        userDao.insertUser(
            UserEntity("res_2", "9876543211", "Alice Smith", "alice@example.com", UserRole.RESIDENT, "comm_1", "flat_2")
        )
        // Security 1 in Community 1
        userDao.insertUser(
            UserEntity("sec_1", "9876543212", "Officer Guard", "guard@example.com", UserRole.SECURITY, "comm_1", null)
        )
        // Security 2 in Community 2
        userDao.insertUser(
            UserEntity("sec_2", "9876543213", "Officer Remote", "remote@example.com", UserRole.SECURITY, "comm_2", null)
        )
    }

    @Test
    fun createVisitor_derivesIdentityFromSessionAndSetsInitialState() = runTest {
        seedCommunitiesAndUsers()
        sessionManager.saveSession("res_1", UserRole.RESIDENT)

        val result = repository.createVisitor(
            name = "John Doe",
            phoneNumber = "9888877777",
            purpose = "Guest",
            scheduledArrivalDate = 1700000000000L,
            vehicleNumber = "KA-05-MJ-1234"
        )

        assertTrue(result.isSuccess)
        val visitor = result.getOrThrow()
        assertEquals("John Doe", visitor.name)
        assertEquals("9888877777", visitor.phoneNumber)
        assertEquals("Guest", visitor.purpose)
        assertEquals("KA-05-MJ-1234", visitor.vehicleNumber)
        assertEquals("res_1", visitor.residentId)
        assertEquals("comm_1", visitor.communityId)
        assertEquals("flat_1", visitor.flatId)
        assertEquals(VisitorStatus.PRE_APPROVED, visitor.status)
        assertNull(visitor.checkInTime)
        assertNull(visitor.checkOutTime)
        assertNull(visitor.verifiedBySecurityId)
        assertNull(visitor.updatedAt)
    }

    @Test
    fun residentIsolation_residentACannotRetrieveResidentBVisitor() = runTest {
        seedCommunitiesAndUsers()

        // Resident 1 creates a visitor
        sessionManager.saveSession("res_1", UserRole.RESIDENT)
        val createResult = repository.createVisitor(
            name = "John Doe",
            phoneNumber = "9888877777",
            purpose = "Guest",
            scheduledArrivalDate = 1700000000000L
        )
        val visitorId = createResult.getOrThrow().id

        // Switch session to Resident 2
        sessionManager.saveSession("res_2", UserRole.RESIDENT)

        val listResult = repository.getMyVisitors()
        assertTrue(listResult.getOrThrow().isEmpty())

        val detailResult = repository.getResidentVisitorDetails(visitorId)
        assertTrue(detailResult.isFailure)
        assertTrue(detailResult.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun residentIsolation_residentACannotCancelResidentBVisitor() = runTest {
        seedCommunitiesAndUsers()

        // Resident 1 creates visitor
        sessionManager.saveSession("res_1", UserRole.RESIDENT)
        val visitorId = repository.createVisitor(
            name = "John Doe",
            phoneNumber = "9888877777",
            purpose = "Guest",
            scheduledArrivalDate = 1700000000000L
        ).getOrThrow().id

        // Resident 2 attempts cancellation
        sessionManager.saveSession("res_2", UserRole.RESIDENT)
        val cancelResult = repository.cancelVisitor(visitorId)
        assertTrue(cancelResult.isFailure)
        assertTrue(cancelResult.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun communityIsolation_securityCommunityACannotAccessOrActOnCommunityBVisitor() = runTest {
        seedCommunitiesAndUsers()

        // Resident 1 (Community 1) creates visitor
        sessionManager.saveSession("res_1", UserRole.RESIDENT)
        val visitorId = repository.createVisitor(
            name = "John Doe",
            phoneNumber = "9888877777",
            purpose = "Guest",
            scheduledArrivalDate = 1700000000000L
        ).getOrThrow().id

        // Security Officer from Community 2 attempts to view / check-in
        sessionManager.saveSession("sec_2", UserRole.SECURITY)

        val listResult = repository.getSecurityVisitors()
        assertTrue(listResult.getOrThrow().isEmpty())

        val detailResult = repository.getSecurityVisitorDetails(visitorId)
        assertTrue(detailResult.isFailure)
        assertTrue(detailResult.exceptionOrNull() is NoSuchElementException)

        val checkInResult = repository.checkInVisitor(visitorId)
        assertTrue(checkInResult.isFailure)
        assertTrue(checkInResult.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun roleAuthorization_residentCannotInvokeSecurityActions() = runTest {
        seedCommunitiesAndUsers()
        sessionManager.saveSession("res_1", UserRole.RESIDENT)

        val visitorId = repository.createVisitor(
            name = "John Doe",
            phoneNumber = "9888877777",
            purpose = "Guest",
            scheduledArrivalDate = 1700000000000L
        ).getOrThrow().id

        val checkInResult = repository.checkInVisitor(visitorId)
        assertTrue(checkInResult.isFailure)
        assertTrue(checkInResult.exceptionOrNull() is SecurityException)

        val checkOutResult = repository.checkOutVisitor(visitorId)
        assertTrue(checkOutResult.isFailure)
        assertTrue(checkOutResult.exceptionOrNull() is SecurityException)

        val denyResult = repository.denyVisitor(visitorId)
        assertTrue(denyResult.isFailure)
        assertTrue(denyResult.exceptionOrNull() is SecurityException)
    }

    @Test
    fun roleAuthorization_securityCannotCreateOrCancelVisitors() = runTest {
        seedCommunitiesAndUsers()
        sessionManager.saveSession("sec_1", UserRole.SECURITY)

        val createResult = repository.createVisitor(
            name = "Illegal Visitor",
            phoneNumber = "9888877777",
            purpose = "Test",
            scheduledArrivalDate = 1700000000000L
        )
        assertTrue(createResult.isFailure)
        assertTrue(createResult.exceptionOrNull() is SecurityException)

        val cancelResult = repository.cancelVisitor("some_id")
        assertTrue(cancelResult.isFailure)
        assertTrue(cancelResult.exceptionOrNull() is SecurityException)
    }

    @Test
    fun validStateTransitions_checkIn_checkOut_lifecycle() = runTest {
        seedCommunitiesAndUsers()

        // 1. Resident creates PRE_APPROVED
        sessionManager.saveSession("res_1", UserRole.RESIDENT)
        val visitor = repository.createVisitor(
            name = "John Doe",
            phoneNumber = "9888877777",
            purpose = "Guest",
            scheduledArrivalDate = 1700000000000L
        ).getOrThrow()
        assertEquals(VisitorStatus.PRE_APPROVED, visitor.status)

        // 2. Security checks in (PRE_APPROVED -> CHECKED_IN)
        sessionManager.saveSession("sec_1", UserRole.SECURITY)
        val checkInResult = repository.checkInVisitor(visitor.id)
        assertTrue(checkInResult.isSuccess)
        val checkedIn = checkInResult.getOrThrow()
        assertEquals(VisitorStatus.CHECKED_IN, checkedIn.status)
        assertNotNull(checkedIn.checkInTime)
        assertEquals("sec_1", checkedIn.verifiedBySecurityId)
        assertNotNull(checkedIn.updatedAt)

        // 3. Security checks out (CHECKED_IN -> CHECKED_OUT)
        val checkOutResult = repository.checkOutVisitor(visitor.id)
        assertTrue(checkOutResult.isSuccess)
        val checkedOut = checkOutResult.getOrThrow()
        assertEquals(VisitorStatus.CHECKED_OUT, checkedOut.status)
        assertNotNull(checkedOut.checkOutTime)
        assertNotNull(checkedOut.updatedAt)
    }

    @Test
    fun validStateTransitions_denyEntry() = runTest {
        seedCommunitiesAndUsers()

        sessionManager.saveSession("res_1", UserRole.RESIDENT)
        val visitor = repository.createVisitor(
            name = "Suspicious",
            phoneNumber = "9888877777",
            purpose = "Unknown",
            scheduledArrivalDate = 1700000000000L
        ).getOrThrow()

        sessionManager.saveSession("sec_1", UserRole.SECURITY)
        val denyResult = repository.denyVisitor(visitor.id)
        assertTrue(denyResult.isSuccess)
        val denied = denyResult.getOrThrow()
        assertEquals(VisitorStatus.DENIED, denied.status)
        assertEquals("sec_1", denied.verifiedBySecurityId)
        assertNotNull(denied.updatedAt)
    }

    @Test
    fun validStateTransitions_residentCancel() = runTest {
        seedCommunitiesAndUsers()

        sessionManager.saveSession("res_1", UserRole.RESIDENT)
        val visitor = repository.createVisitor(
            name = "John Doe",
            phoneNumber = "9888877777",
            purpose = "Guest",
            scheduledArrivalDate = 1700000000000L
        ).getOrThrow()

        val cancelResult = repository.cancelVisitor(visitor.id)
        assertTrue(cancelResult.isSuccess)
        val cancelled = cancelResult.getOrThrow()
        assertEquals(VisitorStatus.CANCELLED, cancelled.status)
        assertNotNull(cancelled.updatedAt)
    }

    @Test
    fun invalidStateTransitions_failDeterministically() = runTest {
        seedCommunitiesAndUsers()

        // Create visitor
        sessionManager.saveSession("res_1", UserRole.RESIDENT)
        val visitor = repository.createVisitor(
            name = "John Doe",
            phoneNumber = "9888877777",
            purpose = "Guest",
            scheduledArrivalDate = 1700000000000L
        ).getOrThrow()

        sessionManager.saveSession("sec_1", UserRole.SECURITY)

        // Invalid: PRE_APPROVED -> CHECKED_OUT
        val checkOutBeforeCheckIn = repository.checkOutVisitor(visitor.id)
        assertTrue(checkOutBeforeCheckIn.isFailure)
        assertTrue(checkOutBeforeCheckIn.exceptionOrNull() is IllegalStateException)

        // Check in
        repository.checkInVisitor(visitor.id)

        // Invalid: CHECKED_IN -> DENIED
        val denyAfterCheckIn = repository.denyVisitor(visitor.id)
        assertTrue(denyAfterCheckIn.isFailure)
        assertTrue(denyAfterCheckIn.exceptionOrNull() is IllegalStateException)

        // Invalid: CHECKED_IN -> CANCELLED
        sessionManager.saveSession("res_1", UserRole.RESIDENT)
        val cancelAfterCheckIn = repository.cancelVisitor(visitor.id)
        assertTrue(cancelAfterCheckIn.isFailure)
        assertTrue(cancelAfterCheckIn.exceptionOrNull() is IllegalStateException)

        // Check out
        sessionManager.saveSession("sec_1", UserRole.SECURITY)
        repository.checkOutVisitor(visitor.id)

        // Invalid: CHECKED_OUT -> CHECKED_IN
        val checkInAfterCheckOut = repository.checkInVisitor(visitor.id)
        assertTrue(checkInAfterCheckOut.isFailure)
        assertTrue(checkInAfterCheckOut.exceptionOrNull() is IllegalStateException)

        // Invalid: CHECKED_OUT -> DENIED
        val denyAfterCheckOut = repository.denyVisitor(visitor.id)
        assertTrue(denyAfterCheckOut.isFailure)
        assertTrue(denyAfterCheckOut.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun securityVisitorDetailResolution_resolvesHostAndFlatData() = runTest {
        seedCommunitiesAndUsers()

        // Resident 1 (David Miller, Flat B-304, Block B, Orchard Heights) creates visitor
        sessionManager.saveSession("res_1", UserRole.RESIDENT)
        val visitor = repository.createVisitor(
            name = "John Doe",
            phoneNumber = "9888877777",
            purpose = "Guest",
            scheduledArrivalDate = 1700000000000L
        ).getOrThrow()

        // Security queries details
        sessionManager.saveSession("sec_1", UserRole.SECURITY)
        val detailResult = repository.getSecurityVisitorDetails(visitor.id)
        assertTrue(detailResult.isSuccess)
        val detail = detailResult.getOrThrow()

        assertEquals("John Doe", detail.visitor.name)
        assertEquals("David Miller", detail.residentName)
        assertEquals("9876543210", detail.residentPhone)
        assertEquals("B-304", detail.flatNumber)
        assertEquals("Block B", detail.block)
        assertEquals("Orchard Heights Apartments", detail.communityName)
    }
}
