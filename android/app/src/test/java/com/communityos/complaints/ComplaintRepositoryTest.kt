package com.communityos.complaints

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.communityos.complaints.model.ComplaintStatus
import com.communityos.complaints.repository.ComplaintRepository
import com.communityos.complaints.repository.ComplaintRepositoryImpl
import com.communityos.data.local.CommunityDatabase
import com.communityos.data.local.dao.ComplaintDao
import com.communityos.data.local.dao.UserDao
import com.communityos.data.local.entity.ComplaintEntity
import com.communityos.data.local.entity.UserEntity
import com.communityos.data.local.session.SessionManager
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
class ComplaintRepositoryTest {

    private lateinit var database: CommunityDatabase
    private lateinit var userDao: UserDao
    private lateinit var complaintDao: ComplaintDao
    private lateinit var sessionManager: SessionManager
    private lateinit var repository: ComplaintRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CommunityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = database.userDao()
        complaintDao = database.complaintDao()

        val testFile = context.preferencesDataStoreFile("test_complaint_repo_${System.nanoTime()}")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { testFile }
        )
        sessionManager = SessionManager(testDataStore)

        repository = ComplaintRepositoryImpl(
            complaintDao = complaintDao,
            userDao = userDao,
            sessionManager = sessionManager
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun createComplaint_resolvesUserFromSession_andSetsInitialStatusSubmitted() = runTest {
        val user = UserEntity(
            id = "user_resident_1",
            phoneNumber = "9876543210",
            name = "Test Resident",
            communityId = "community_orchard",
            flatId = "flat_b304",
            role = UserRole.RESIDENT
        )
        userDao.insertUser(user)
        sessionManager.saveSession("user_resident_1", UserRole.RESIDENT)

        val result = repository.createComplaint("Water", "Low water pressure in kitchen")
        assertTrue(result.isSuccess)
        val complaint = result.getOrNull()
        assertNotNull(complaint)
        assertEquals("user_resident_1", complaint?.residentId)
        assertEquals("community_orchard", complaint?.communityId)
        assertEquals("flat_b304", complaint?.flatId)
        assertEquals("Water", complaint?.category)
        assertEquals("Low water pressure in kitchen", complaint?.description)
        assertEquals(ComplaintStatus.SUBMITTED, complaint?.status)
        assertNull(complaint?.updatedAt)

        // Verify persisted in DAO
        val persisted = complaintDao.getComplaintsForResident("user_resident_1")
        assertEquals(1, persisted.size)
        assertEquals(complaint?.id, persisted[0].id)
        assertNull(persisted[0].updatedAt)
    }

    @Test
    fun createComplaint_failsWhenNoActiveSession() = runTest {
        val result = repository.createComplaint("Electricity", "Power outage")
        assertTrue(result.isFailure)
        assertEquals("No active session found", result.exceptionOrNull()?.message)
    }

    @Test
    fun createComplaint_failsWhenUserNotInDatabase() = runTest {
        sessionManager.saveSession("unknown_user_id", UserRole.RESIDENT)
        val result = repository.createComplaint("Electricity", "Power outage")
        assertTrue(result.isFailure)
        assertEquals("User not found for active session", result.exceptionOrNull()?.message)
    }

    @Test
    fun createComplaint_failsWhenUserHasNoCommunity() = runTest {
        val user = UserEntity(
            id = "user_no_comm",
            phoneNumber = "1234567890",
            name = "No Community User",
            communityId = null,
            flatId = "flat_101"
        )
        userDao.insertUser(user)
        sessionManager.saveSession("user_no_comm", UserRole.RESIDENT)

        val result = repository.createComplaint("Maintenance", "Elevator broken")
        assertTrue(result.isFailure)
        assertEquals("User is not associated with any community", result.exceptionOrNull()?.message)
    }

    @Test
    fun createComplaint_failsWhenUserHasNoFlat() = runTest {
        val user = UserEntity(
            id = "user_no_flat",
            phoneNumber = "1234567890",
            name = "No Flat User",
            communityId = "comm_1",
            flatId = null
        )
        userDao.insertUser(user)
        sessionManager.saveSession("user_no_flat", UserRole.RESIDENT)

        val result = repository.createComplaint("Maintenance", "Elevator broken")
        assertTrue(result.isFailure)
        assertEquals("User is not associated with any flat", result.exceptionOrNull()?.message)
    }

    @Test
    fun createComplaint_failsWhenCategoryOrDescriptionBlank() = runTest {
        val user = UserEntity(
            id = "user_1",
            phoneNumber = "9876543210",
            name = "Test Resident",
            communityId = "comm_1",
            flatId = "flat_1"
        )
        userDao.insertUser(user)
        sessionManager.saveSession("user_1", UserRole.RESIDENT)

        val blankCat = repository.createComplaint("   ", "Valid description")
        assertTrue(blankCat.isFailure)
        assertTrue(blankCat.exceptionOrNull() is IllegalArgumentException)

        val blankDesc = repository.createComplaint("Water", "   ")
        assertTrue(blankDesc.isFailure)
        assertTrue(blankDesc.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun getMyComplaints_returnsOnlyActiveResidentComplaints() = runTest {
        val userA = UserEntity(
            id = "resident_A",
            phoneNumber = "1111111111",
            name = "Resident A",
            communityId = "comm_1",
            flatId = "flat_A"
        )
        val userB = UserEntity(
            id = "resident_B",
            phoneNumber = "2222222222",
            name = "Resident B",
            communityId = "comm_1",
            flatId = "flat_B"
        )
        userDao.insertUser(userA)
        userDao.insertUser(userB)

        complaintDao.insertComplaint(
            ComplaintEntity(
                id = "c_A",
                residentId = "resident_A",
                communityId = "comm_1",
                flatId = "flat_A",
                category = "Water",
                description = "Tap leaking",
                status = ComplaintStatus.SUBMITTED,
                createdAt = 1000L
            )
        )
        complaintDao.insertComplaint(
            ComplaintEntity(
                id = "c_B",
                residentId = "resident_B",
                communityId = "comm_1",
                flatId = "flat_B",
                category = "Electricity",
                description = "Bulb broken",
                status = ComplaintStatus.SUBMITTED,
                createdAt = 2000L
            )
        )

        sessionManager.saveSession("resident_A", UserRole.RESIDENT)
        val result = repository.getMyComplaints()
        assertTrue(result.isSuccess)
        val list = result.getOrNull()
        assertEquals(1, list?.size)
        assertEquals("c_A", list?.get(0)?.id)
        assertEquals("Tap leaking", list?.get(0)?.description)
    }

    @Test
    fun getComplaintDetails_failsWhenAccessingAnotherResidentsComplaint() = runTest {
        val userA = UserEntity(
            id = "resident_A",
            phoneNumber = "1111111111",
            name = "Resident A",
            communityId = "comm_1",
            flatId = "flat_A"
        )
        userDao.insertUser(userA)

        complaintDao.insertComplaint(
            ComplaintEntity(
                id = "c_other",
                residentId = "resident_OTHER",
                communityId = "comm_1",
                flatId = "flat_OTHER",
                category = "Security",
                description = "Other resident's issue",
                status = ComplaintStatus.SUBMITTED,
                createdAt = 1000L
            )
        )

        sessionManager.saveSession("resident_A", UserRole.RESIDENT)
        val result = repository.getComplaintDetails("c_other")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun getComplaintDetails_succeedsWhenBelongingToCurrentResident() = runTest {
        val userA = UserEntity(
            id = "resident_A",
            phoneNumber = "1111111111",
            name = "Resident A",
            communityId = "comm_1",
            flatId = "flat_A"
        )
        userDao.insertUser(userA)

        complaintDao.insertComplaint(
            ComplaintEntity(
                id = "c_mine",
                residentId = "resident_A",
                communityId = "comm_1",
                flatId = "flat_A",
                category = "Cleanliness",
                description = "Trash not collected",
                status = ComplaintStatus.SUBMITTED,
                createdAt = 1000L
            )
        )

        sessionManager.saveSession("resident_A", UserRole.RESIDENT)
        val result = repository.getComplaintDetails("c_mine")
        assertTrue(result.isSuccess)
        assertEquals("c_mine", result.getOrNull()?.id)
        assertEquals("Trash not collected", result.getOrNull()?.description)
    }
}
