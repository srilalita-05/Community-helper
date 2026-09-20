package com.communityos.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.communityos.data.local.dao.VisitorDao
import com.communityos.data.local.entity.VisitorEntity
import com.communityos.visitors.model.VisitorStatus
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
class VisitorDaoTest {

    private lateinit var database: CommunityDatabase
    private lateinit var visitorDao: VisitorDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CommunityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        visitorDao = database.visitorDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveVisitor_scopedToResident() = runTest {
        val visitor = VisitorEntity(
            id = "v1",
            residentId = "res_1",
            communityId = "comm_1",
            flatId = "flat_1",
            name = "John Doe",
            phoneNumber = "9888877777",
            purpose = "Guest",
            vehicleNumber = "KA-05-MJ-1234",
            scheduledArrivalDate = 1700000000000L,
            status = VisitorStatus.PRE_APPROVED,
            createdAt = 1000L
        )
        visitorDao.insertVisitor(visitor)

        val resident1Visitors = visitorDao.getVisitorsForResident("res_1")
        assertEquals(1, resident1Visitors.size)
        assertEquals("John Doe", resident1Visitors[0].name)
        assertEquals("KA-05-MJ-1234", resident1Visitors[0].vehicleNumber)
        assertEquals(VisitorStatus.PRE_APPROVED, resident1Visitors[0].status)

        // Scoping: Resident 2 gets 0 visitors
        val resident2Visitors = visitorDao.getVisitorsForResident("res_2")
        assertTrue(resident2Visitors.isEmpty())
    }

    @Test
    fun residentDetailLookup_strictlyScoped() = runTest {
        val visitor = VisitorEntity(
            id = "v1",
            residentId = "res_1",
            communityId = "comm_1",
            flatId = "flat_1",
            name = "John Doe",
            phoneNumber = "9888877777",
            purpose = "Guest",
            scheduledArrivalDate = 1700000000000L,
            status = VisitorStatus.PRE_APPROVED,
            createdAt = 1000L
        )
        visitorDao.insertVisitor(visitor)

        // Correct resident retrieves visitor
        val retrieved = visitorDao.getVisitorByIdAndResident("v1", "res_1")
        assertNotNull(retrieved)
        assertEquals("John Doe", retrieved?.name)

        // Other resident cannot retrieve visitor
        val unauthorized = visitorDao.getVisitorByIdAndResident("v1", "res_2")
        assertNull(unauthorized)
    }

    @Test
    fun communityScoping_andDetailLookup() = runTest {
        val visitorComm1 = VisitorEntity(
            id = "v_comm1",
            residentId = "res_1",
            communityId = "comm_1",
            flatId = "flat_1",
            name = "Visitor 1",
            phoneNumber = "9888877771",
            purpose = "Delivery",
            scheduledArrivalDate = 1700000000000L,
            status = VisitorStatus.PRE_APPROVED,
            createdAt = 1000L
        )
        val visitorComm2 = VisitorEntity(
            id = "v_comm2",
            residentId = "res_2",
            communityId = "comm_2",
            flatId = "flat_2",
            name = "Visitor 2",
            phoneNumber = "9888877772",
            purpose = "Cab",
            scheduledArrivalDate = 1700000000000L,
            status = VisitorStatus.PRE_APPROVED,
            createdAt = 2000L
        )
        visitorDao.insertVisitors(listOf(visitorComm1, visitorComm2))

        val comm1Visitors = visitorDao.getVisitorsForCommunity("comm_1")
        assertEquals(1, comm1Visitors.size)
        assertEquals("v_comm1", comm1Visitors[0].id)

        val comm2Visitors = visitorDao.getVisitorsForCommunity("comm_2")
        assertEquals(1, comm2Visitors.size)
        assertEquals("v_comm2", comm2Visitors[0].id)

        // Detail lookup scoped to community
        val foundComm1 = visitorDao.getVisitorByIdAndCommunity("v_comm1", "comm_1")
        assertNotNull(foundComm1)

        val notFoundInComm2 = visitorDao.getVisitorByIdAndCommunity("v_comm1", "comm_2")
        assertNull(notFoundInComm2)
    }

    @Test
    fun visitorsOrdering_createdAtDescending() = runTest {
        val v1 = VisitorEntity(
            id = "v1",
            residentId = "res_1",
            communityId = "comm_1",
            flatId = "flat_1",
            name = "First",
            phoneNumber = "9888877771",
            purpose = "Guest",
            scheduledArrivalDate = 1700000000000L,
            status = VisitorStatus.PRE_APPROVED,
            createdAt = 1000L
        )
        val v2 = VisitorEntity(
            id = "v2",
            residentId = "res_1",
            communityId = "comm_1",
            flatId = "flat_1",
            name = "Second",
            phoneNumber = "9888877772",
            purpose = "Guest",
            scheduledArrivalDate = 1700000000000L,
            status = VisitorStatus.PRE_APPROVED,
            createdAt = 2000L
        )
        visitorDao.insertVisitors(listOf(v1, v2))

        val list = visitorDao.getVisitorsForResident("res_1")
        assertEquals(2, list.size)
        assertEquals("v2", list[0].id) // newer first
        assertEquals("v1", list[1].id)
    }

    @Test
    fun statusFiltering_forResidentAndSecurity() = runTest {
        val v1 = VisitorEntity(
            id = "v1",
            residentId = "res_1",
            communityId = "comm_1",
            flatId = "flat_1",
            name = "Pre-approved",
            phoneNumber = "9888877771",
            purpose = "Guest",
            scheduledArrivalDate = 1700000000000L,
            status = VisitorStatus.PRE_APPROVED,
            createdAt = 1000L
        )
        val v2 = VisitorEntity(
            id = "v2",
            residentId = "res_1",
            communityId = "comm_1",
            flatId = "flat_1",
            name = "Checked-in",
            phoneNumber = "9888877772",
            purpose = "Guest",
            scheduledArrivalDate = 1700000000000L,
            status = VisitorStatus.CHECKED_IN,
            createdAt = 2000L
        )
        val v3 = VisitorEntity(
            id = "v3",
            residentId = "res_1",
            communityId = "comm_1",
            flatId = "flat_1",
            name = "Checked-out",
            phoneNumber = "9888877773",
            purpose = "Guest",
            scheduledArrivalDate = 1700000000000L,
            status = VisitorStatus.CHECKED_OUT,
            createdAt = 3000L
        )
        visitorDao.insertVisitors(listOf(v1, v2, v3))

        val preApprovedResident = visitorDao.observeVisitorsForResidentByStatus("res_1", VisitorStatus.PRE_APPROVED).first()
        assertEquals(1, preApprovedResident.size)
        assertEquals("v1", preApprovedResident[0].id)

        val preApprovedComm = visitorDao.observeVisitorsForCommunityByStatus("comm_1", VisitorStatus.PRE_APPROVED).first()
        assertEquals(1, preApprovedComm.size)
        assertEquals("v1", preApprovedComm[0].id)

        val historyComm = visitorDao.observeVisitorsForCommunityByStatuses(
            "comm_1",
            listOf(VisitorStatus.CHECKED_OUT, VisitorStatus.DENIED, VisitorStatus.CANCELLED)
        ).first()
        assertEquals(1, historyComm.size)
        assertEquals("v3", historyComm[0].id)
    }

    @Test
    fun activeVisitorCounts_calculatedCorrectly() = runTest {
        val v1 = VisitorEntity(
            id = "v1",
            residentId = "res_1",
            communityId = "comm_1",
            flatId = "flat_1",
            name = "Inside 1",
            phoneNumber = "9888877771",
            purpose = "Guest",
            scheduledArrivalDate = 1700000000000L,
            status = VisitorStatus.CHECKED_IN,
            createdAt = 1000L
        )
        val v2 = VisitorEntity(
            id = "v2",
            residentId = "res_1",
            communityId = "comm_1",
            flatId = "flat_1",
            name = "Inside 2",
            phoneNumber = "9888877772",
            purpose = "Guest",
            scheduledArrivalDate = 1700000000000L,
            status = VisitorStatus.CHECKED_IN,
            createdAt = 2000L
        )
        val v3 = VisitorEntity(
            id = "v3",
            residentId = "res_1",
            communityId = "comm_1",
            flatId = "flat_1",
            name = "Expected",
            phoneNumber = "9888877773",
            purpose = "Guest",
            scheduledArrivalDate = 1700000000000L,
            status = VisitorStatus.PRE_APPROVED,
            createdAt = 3000L
        )
        val v4 = VisitorEntity(
            id = "v4",
            residentId = "res_2",
            communityId = "comm_1",
            flatId = "flat_2",
            name = "Inside Res 2",
            phoneNumber = "9888877774",
            purpose = "Guest",
            scheduledArrivalDate = 1700000000000L,
            status = VisitorStatus.CHECKED_IN,
            createdAt = 4000L
        )
        visitorDao.insertVisitors(listOf(v1, v2, v3, v4))

        // Resident 1 active count (only CHECKED_IN for res_1)
        val res1Active = visitorDao.countActiveVisitorsForResident("res_1")
        assertEquals(2, res1Active)

        // Resident 2 active count
        val res2Active = visitorDao.countActiveVisitorsForResident("res_2")
        assertEquals(1, res2Active)

        // Community 1 active count (all CHECKED_IN for comm_1)
        val comm1Active = visitorDao.countActiveVisitorsForCommunity("comm_1")
        assertEquals(3, comm1Active)
    }

    @Test
    fun updateVisitor_persistsChanges() = runTest {
        val v1 = VisitorEntity(
            id = "v1",
            residentId = "res_1",
            communityId = "comm_1",
            flatId = "flat_1",
            name = "John",
            phoneNumber = "9888877771",
            purpose = "Guest",
            scheduledArrivalDate = 1700000000000L,
            status = VisitorStatus.PRE_APPROVED,
            createdAt = 1000L
        )
        visitorDao.insertVisitor(v1)

        val updated = v1.copy(
            status = VisitorStatus.CHECKED_IN,
            checkInTime = 2000L,
            verifiedBySecurityId = "sec_1",
            updatedAt = 2000L
        )
        visitorDao.updateVisitor(updated)

        val retrieved = visitorDao.getVisitorByIdAndResident("v1", "res_1")
        assertEquals(VisitorStatus.CHECKED_IN, retrieved?.status)
        assertEquals(2000L, retrieved?.checkInTime)
        assertEquals("sec_1", retrieved?.verifiedBySecurityId)
        assertEquals(2000L, retrieved?.updatedAt)
    }
}
