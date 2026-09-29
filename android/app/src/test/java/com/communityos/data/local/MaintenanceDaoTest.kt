package com.communityos.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.communityos.data.local.dao.MaintenanceDao
import com.communityos.data.local.entity.CommunityEntity
import com.communityos.data.local.entity.FlatEntity
import com.communityos.data.local.entity.MaintenanceBillEntity
import com.communityos.data.local.entity.MaintenancePaymentEntity
import com.communityos.data.local.entity.UserEntity
import com.communityos.maintenance.model.BillStatus
import com.communityos.maintenance.model.PaymentMethod
import com.communityos.models.UserRole
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
class MaintenanceDaoTest {

    private lateinit var database: CommunityDatabase
    private lateinit var maintenanceDao: MaintenanceDao

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CommunityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        maintenanceDao = database.maintenanceDao()

        // Insert prerequisite foreign keys (community, flat, user)
        database.communityDao().insertCommunity(
            CommunityEntity("comm_1", "Greenwood Apartments", "123 Main St", "Bengaluru", 4)
        )

        database.flatDao().insertFlat(
            FlatEntity("flat_1", "comm_1", "Block A", "A-101", 1)
        )
        database.flatDao().insertFlat(
            FlatEntity("flat_2", "comm_1", "Block B", "B-202", 2)
        )

        database.userDao().insertUser(
            UserEntity(
                id = "res_1",
                phoneNumber = "9876543210",
                name = "Alice",
                email = "alice@example.com",
                role = UserRole.RESIDENT,
                communityId = "comm_1",
                flatId = "flat_1",
                isApproved = true,
                createdAt = 1000L
            )
        )
        database.userDao().insertUser(
            UserEntity(
                id = "res_2",
                phoneNumber = "9888877777",
                name = "Bob",
                email = "bob@example.com",
                role = UserRole.RESIDENT,
                communityId = "comm_1",
                flatId = "flat_2",
                isApproved = true,
                createdAt = 1000L
            )
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun getBillsForFlat_returnsOnlyBillsForSpecifiedFlat() = runTest {
        val billFlat1 = MaintenanceBillEntity(
            id = "bill_1",
            flatId = "flat_1",
            communityId = "comm_1",
            title = "Oct 2026 Maintenance",
            period = "October 2026",
            amount = 2500.0,
            dueDate = 1794268800000L,
            status = BillStatus.UNPAID,
            createdAt = 1000L
        )
        val billFlat2 = MaintenanceBillEntity(
            id = "bill_2",
            flatId = "flat_2",
            communityId = "comm_1",
            title = "Oct 2026 Maintenance",
            period = "October 2026",
            amount = 3000.0,
            dueDate = 1794268800000L,
            status = BillStatus.UNPAID,
            createdAt = 1000L
        )

        maintenanceDao.insertBills(listOf(billFlat1, billFlat2))

        val flat1Bills = maintenanceDao.getBillsForFlat("flat_1")
        assertEquals(1, flat1Bills.size)
        assertEquals("bill_1", flat1Bills[0].id)
        assertEquals(2500.0, flat1Bills[0].amount, 0.01)

        val flat2Bills = maintenanceDao.getBillsForFlat("flat_2")
        assertEquals(1, flat2Bills.size)
        assertEquals("bill_2", flat2Bills[0].id)
    }

    @Test
    fun getUnpaidBillsForFlat_returnsOnlyUnpaidAndOverdueBills() = runTest {
        val unpaidBill = MaintenanceBillEntity(
            id = "b_unpaid",
            flatId = "flat_1",
            communityId = "comm_1",
            title = "Oct 2026 Maintenance",
            period = "October 2026",
            amount = 2500.0,
            dueDate = 2000L,
            status = BillStatus.UNPAID,
            createdAt = 1000L
        )
        val overdueBill = MaintenanceBillEntity(
            id = "b_overdue",
            flatId = "flat_1",
            communityId = "comm_1",
            title = "Sep 2026 Levy",
            period = "September 2026",
            amount = 1200.0,
            dueDate = 1500L,
            status = BillStatus.OVERDUE,
            createdAt = 900L
        )
        val paidBill = MaintenanceBillEntity(
            id = "b_paid",
            flatId = "flat_1",
            communityId = "comm_1",
            title = "Aug 2026 Maintenance",
            period = "August 2026",
            amount = 2500.0,
            dueDate = 1000L,
            status = BillStatus.PAID,
            createdAt = 800L
        )

        maintenanceDao.insertBills(listOf(unpaidBill, overdueBill, paidBill))

        val unpaidBills = maintenanceDao.getUnpaidBillsForFlat("flat_1")
        assertEquals(2, unpaidBills.size)
        assertTrue(unpaidBills.any { it.id == "b_unpaid" })
        assertTrue(unpaidBills.any { it.id == "b_overdue" })
        assertFalse(unpaidBills.any { it.id == "b_paid" })

        // Check Flow observation
        val observedUnpaid = maintenanceDao.observeUnpaidBillsForFlat("flat_1").first()
        assertEquals(2, observedUnpaid.size)
    }

    @Test
    fun getPaidBillsForFlat_returnsOnlyPaidBills() = runTest {
        val unpaidBill = MaintenanceBillEntity(
            id = "b_unpaid",
            flatId = "flat_1",
            communityId = "comm_1",
            title = "Oct 2026 Maintenance",
            period = "October 2026",
            amount = 2500.0,
            dueDate = 2000L,
            status = BillStatus.UNPAID,
            createdAt = 1000L
        )
        val paidBill = MaintenanceBillEntity(
            id = "b_paid",
            flatId = "flat_1",
            communityId = "comm_1",
            title = "Aug 2026 Maintenance",
            period = "August 2026",
            amount = 2500.0,
            dueDate = 1000L,
            status = BillStatus.PAID,
            createdAt = 800L
        )

        maintenanceDao.insertBills(listOf(unpaidBill, paidBill))

        val paidBills = maintenanceDao.getPaidBillsForFlat("flat_1")
        assertEquals(1, paidBills.size)
        assertEquals("b_paid", paidBills[0].id)
    }

    @Test
    fun getBillByIdAndFlat_enforcesFlatOwnership() = runTest {
        val bill = MaintenanceBillEntity(
            id = "bill_secret",
            flatId = "flat_1",
            communityId = "comm_1",
            title = "Special Assessment",
            period = "October 2026",
            amount = 5000.0,
            dueDate = 2000L,
            status = BillStatus.UNPAID,
            createdAt = 1000L
        )
        maintenanceDao.insertBill(bill)

        // Lookup with correct flatId succeeds
        val foundBill = maintenanceDao.getBillByIdAndFlat("bill_secret", "flat_1")
        assertNotNull(foundBill)
        assertEquals("bill_secret", foundBill?.id)

        // Lookup with different flatId returns null (enforcing strict isolation)
        val unauthorizedLookup = maintenanceDao.getBillByIdAndFlat("bill_secret", "flat_2")
        assertNull(unauthorizedLookup)
    }

    @Test
    fun getUnpaidTotalForFlat_calculatesCorrectSum() = runTest {
        val bill1 = MaintenanceBillEntity(
            id = "b1",
            flatId = "flat_1",
            communityId = "comm_1",
            title = "Oct Maintenance",
            period = "October 2026",
            amount = 2500.0,
            dueDate = 2000L,
            status = BillStatus.UNPAID,
            createdAt = 1000L
        )
        val bill2 = MaintenanceBillEntity(
            id = "b2",
            flatId = "flat_1",
            communityId = "comm_1",
            title = "Sep Levy",
            period = "September 2026",
            amount = 1200.0,
            dueDate = 1500L,
            status = BillStatus.OVERDUE,
            createdAt = 900L
        )
        val bill3 = MaintenanceBillEntity(
            id = "b3",
            flatId = "flat_1",
            communityId = "comm_1",
            title = "Aug Maintenance",
            period = "August 2026",
            amount = 2500.0,
            dueDate = 1000L,
            status = BillStatus.PAID,
            createdAt = 800L
        )

        maintenanceDao.insertBills(listOf(bill1, bill2, bill3))

        val totalFlat1 = maintenanceDao.getUnpaidTotalForFlat("flat_1")
        assertEquals(3700.0, totalFlat1, 0.01)

        val totalFlat2 = maintenanceDao.getUnpaidTotalForFlat("flat_2")
        assertEquals(0.0, totalFlat2, 0.01)
    }

    @Test
    fun insertPayment_and_getPaymentsForFlat() = runTest {
        val bill = MaintenanceBillEntity(
            id = "bill_p1",
            flatId = "flat_1",
            communityId = "comm_1",
            title = "Oct Maintenance",
            period = "October 2026",
            amount = 2500.0,
            dueDate = 2000L,
            status = BillStatus.UNPAID,
            createdAt = 1000L
        )
        maintenanceDao.insertBill(bill)

        val payment = MaintenancePaymentEntity(
            id = "pay_1",
            billId = "bill_p1",
            flatId = "flat_1",
            residentId = "res_1",
            amountPaid = 2500.0,
            paymentMethod = PaymentMethod.UPI_SIMULATED,
            transactionRef = "SIM_TXN_123456",
            paymentDate = 2100L
        )
        maintenanceDao.insertPayment(payment)

        val payments = maintenanceDao.getPaymentsForFlat("flat_1")
        assertEquals(1, payments.size)
        assertEquals("pay_1", payments[0].id)
        assertEquals("SIM_TXN_123456", payments[0].transactionRef)
        assertEquals(PaymentMethod.UPI_SIMULATED, payments[0].paymentMethod)

        val paymentForBill = maintenanceDao.getPaymentForBill("bill_p1")
        assertNotNull(paymentForBill)
        assertEquals("pay_1", paymentForBill?.id)

        val paymentsFlat2 = maintenanceDao.getPaymentsForFlat("flat_2")
        assertTrue(paymentsFlat2.isEmpty())
    }

    @Test
    fun updateBill_updatesStatusToPaid() = runTest {
        val bill = MaintenanceBillEntity(
            id = "bill_u",
            flatId = "flat_1",
            communityId = "comm_1",
            title = "Oct Maintenance",
            period = "October 2026",
            amount = 2500.0,
            dueDate = 2000L,
            status = BillStatus.UNPAID,
            createdAt = 1000L
        )
        maintenanceDao.insertBill(bill)

        val updated = bill.copy(status = BillStatus.PAID, updatedAt = 3000L)
        maintenanceDao.updateBill(updated)

        val fetched = maintenanceDao.getBillByIdAndFlat("bill_u", "flat_1")
        assertNotNull(fetched)
        assertEquals(BillStatus.PAID, fetched?.status)
        assertEquals(3000L, fetched?.updatedAt)
    }
}
