package com.communityos.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.communityos.complaints.model.ComplaintStatus
import com.communityos.data.local.dao.ComplaintDao
import com.communityos.data.local.entity.ComplaintEntity
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
class ComplaintDaoTest {

    private lateinit var database: CommunityDatabase
    private lateinit var complaintDao: ComplaintDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CommunityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        complaintDao = database.complaintDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertComplaintAndRetrieve_byResident() = runTest {
        val complaint = ComplaintEntity(
            id = "c_1",
            residentId = "resident_1",
            communityId = "comm_1",
            flatId = "flat_1",
            category = "Water",
            description = "No water supply in morning",
            status = ComplaintStatus.SUBMITTED,
            createdAt = 1000L
        )
        complaintDao.insertComplaint(complaint)

        val retrieved = complaintDao.getComplaintsForResident("resident_1")
        assertEquals(1, retrieved.size)
        assertEquals("c_1", retrieved[0].id)
        assertEquals("Water", retrieved[0].category)
        assertEquals("No water supply in morning", retrieved[0].description)
        assertEquals(ComplaintStatus.SUBMITTED, retrieved[0].status)
        assertNull(retrieved[0].updatedAt)
    }

    @Test
    fun complaintsAreOrderedByCreatedAtDescending() = runTest {
        val cOld = ComplaintEntity(
            id = "c_old",
            residentId = "res_1",
            communityId = "comm_1",
            flatId = "flat_1",
            category = "Electricity",
            description = "Old complaint",
            status = ComplaintStatus.SUBMITTED,
            createdAt = 1000L
        )
        val cNew = ComplaintEntity(
            id = "c_new",
            residentId = "res_1",
            communityId = "comm_1",
            flatId = "flat_1",
            category = "Water",
            description = "New complaint",
            status = ComplaintStatus.SUBMITTED,
            createdAt = 2000L
        )
        complaintDao.insertComplaints(listOf(cOld, cNew))

        val list = complaintDao.getComplaintsForResident("res_1")
        assertEquals(2, list.size)
        assertEquals("c_new", list[0].id)
        assertEquals("c_old", list[1].id)
    }

    @Test
    fun complaintsAreScopedByResidentId_crossResidentAccessPrevented() = runTest {
        val cResA = ComplaintEntity(
            id = "c_a",
            residentId = "resident_A",
            communityId = "comm_1",
            flatId = "flat_A",
            category = "Security",
            description = "Gate lock broken",
            status = ComplaintStatus.SUBMITTED,
            createdAt = 1500L
        )
        val cResB = ComplaintEntity(
            id = "c_b",
            residentId = "resident_B",
            communityId = "comm_1",
            flatId = "flat_B",
            category = "Cleanliness",
            description = "Corridor not cleaned",
            status = ComplaintStatus.SUBMITTED,
            createdAt = 1600L
        )
        complaintDao.insertComplaints(listOf(cResA, cResB))

        val listA = complaintDao.getComplaintsForResident("resident_A")
        assertEquals(1, listA.size)
        assertEquals("c_a", listA[0].id)

        val listB = complaintDao.getComplaintsForResident("resident_B")
        assertEquals(1, listB.size)
        assertEquals("c_b", listB[0].id)
    }

    @Test
    fun getComplaintByIdAndResident_returnsComplaintWhenBothMatch() = runTest {
        val complaint = ComplaintEntity(
            id = "complaint_123",
            residentId = "resident_A",
            communityId = "comm_1",
            flatId = "flat_101",
            category = "Maintenance",
            description = "Elevator issue",
            status = ComplaintStatus.SUBMITTED,
            createdAt = 3000L
        )
        complaintDao.insertComplaint(complaint)

        val found = complaintDao.getComplaintByIdAndResident("complaint_123", "resident_A")
        assertNotNull(found)
        assertEquals("complaint_123", found?.id)
        assertEquals("Maintenance", found?.category)
    }

    @Test
    fun getComplaintByIdAndResident_returnsNullWhenResidentDoesNotMatch() = runTest {
        val complaint = ComplaintEntity(
            id = "complaint_123",
            residentId = "resident_A",
            communityId = "comm_1",
            flatId = "flat_101",
            category = "Maintenance",
            description = "Elevator issue",
            status = ComplaintStatus.SUBMITTED,
            createdAt = 3000L
        )
        complaintDao.insertComplaint(complaint)

        // Resident B attempts to fetch Resident A's complaint
        val found = complaintDao.getComplaintByIdAndResident("complaint_123", "resident_B")
        assertNull(found)
    }

    @Test
    fun statusPersistsCorrectly_acrossTransitions() = runTest {
        val complaint = ComplaintEntity(
            id = "c_status",
            residentId = "resident_A",
            communityId = "comm_1",
            flatId = "flat_101",
            category = "Water",
            description = "Pipe leak",
            status = ComplaintStatus.IN_PROGRESS,
            createdAt = 1000L
        )
        complaintDao.insertComplaint(complaint)

        val retrieved = complaintDao.getComplaintByIdAndResident("c_status", "resident_A")
        assertNotNull(retrieved)
        assertEquals(ComplaintStatus.IN_PROGRESS, retrieved?.status)
    }

    @Test
    fun observeComplaintsForResident_emitsUpdatedList() = runTest {
        val c1 = ComplaintEntity(
            id = "c_obs",
            residentId = "resident_A",
            communityId = "comm_1",
            flatId = "flat_101",
            category = "Other",
            description = "Noise complaint",
            status = ComplaintStatus.SUBMITTED,
            createdAt = 5000L
        )
        complaintDao.insertComplaint(c1)

        val list = complaintDao.observeComplaintsForResident("resident_A").first()
        assertEquals(1, list.size)
        assertEquals("Noise complaint", list[0].description)
    }

    @Test
    fun pendingComplaintsCount_calculatesCorrectly() = runTest {
        val c1 = ComplaintEntity(
            id = "c_1",
            residentId = "resident_A",
            communityId = "comm_1",
            flatId = "flat_1",
            category = "Water",
            description = "Issue 1",
            status = ComplaintStatus.SUBMITTED,
            createdAt = 1000L
        )
        val c2 = ComplaintEntity(
            id = "c_2",
            residentId = "resident_A",
            communityId = "comm_1",
            flatId = "flat_1",
            category = "Electricity",
            description = "Issue 2",
            status = ComplaintStatus.IN_PROGRESS,
            createdAt = 2000L
        )
        val c3 = ComplaintEntity(
            id = "c_3",
            residentId = "resident_A",
            communityId = "comm_1",
            flatId = "flat_1",
            category = "Cleanliness",
            description = "Issue 3",
            status = ComplaintStatus.RESOLVED,
            createdAt = 3000L
        )
        complaintDao.insertComplaints(listOf(c1, c2, c3))

        val pending = complaintDao.getPendingComplaintsCount("resident_A")
        assertEquals(2, pending)
    }
}
