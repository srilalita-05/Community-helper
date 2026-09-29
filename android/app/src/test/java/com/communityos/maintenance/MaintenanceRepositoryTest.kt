package com.communityos.maintenance

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.communityos.data.local.CommunityDatabase
import com.communityos.data.local.dao.CommunityDao
import com.communityos.data.local.dao.FlatDao
import com.communityos.data.local.dao.MaintenanceDao
import com.communityos.data.local.dao.UserDao
import com.communityos.data.local.entity.CommunityEntity
import com.communityos.data.local.entity.FlatEntity
import com.communityos.data.local.entity.MaintenanceBillEntity
import com.communityos.data.local.entity.UserEntity
import com.communityos.data.local.session.SessionManager
import com.communityos.maintenance.model.BillStatus
import com.communityos.maintenance.model.PaymentMethod
import com.communityos.maintenance.repository.MaintenanceRepository
import com.communityos.maintenance.repository.MaintenanceRepositoryImpl
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
class MaintenanceRepositoryTest {

    private lateinit var database: CommunityDatabase
    private lateinit var maintenanceDao: MaintenanceDao
    private lateinit var userDao: UserDao
    private lateinit var flatDao: FlatDao
    private lateinit var communityDao: CommunityDao
    private lateinit var sessionManager: SessionManager
    private lateinit var repository: MaintenanceRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CommunityDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        maintenanceDao = database.maintenanceDao()
        userDao = database.userDao()
        flatDao = database.flatDao()
        communityDao = database.communityDao()

        val testFile = context.preferencesDataStoreFile("test_maint_repo_${System.nanoTime()}")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { testFile }
        )
        sessionManager = SessionManager(testDataStore)

        repository = MaintenanceRepositoryImpl(
            maintenanceDao = maintenanceDao,
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

    private suspend fun seedMasterData() {
        communityDao.insertCommunity(
            CommunityEntity("comm_1", "Greenwood Residency", "123 Park Ave", "Bengaluru", 2)
        )
        communityDao.insertCommunity(
            CommunityEntity("comm_2", "Sunrise Enclave", "456 Lake Rd", "Bengaluru", 2)
        )

        flatDao.insertFlat(FlatEntity("flat_1", "comm_1", "Block A", "A-101", 1))
        flatDao.insertFlat(FlatEntity("flat_2", "comm_1", "Block B", "B-202", 2))
        flatDao.insertFlat(FlatEntity("flat_3", "comm_2", "Block C", "C-303", 3))

        userDao.insertUser(
            UserEntity("res_1", "9876543210", "Alice", "alice@example.com", UserRole.RESIDENT, "comm_1", "flat_1")
        )
        userDao.insertUser(
            UserEntity("res_2", "9876543211", "Bob", "bob@example.com", UserRole.RESIDENT, "comm_1", "flat_2")
        )
        userDao.insertUser(
            UserEntity("admin_1", "9876543212", "Carol Admin", "carol@example.com", UserRole.ADMIN, "comm_1", null)
        )
        userDao.insertUser(
            UserEntity("res_no_flat", "9876543213", "Dave", "dave@example.com", UserRole.RESIDENT, "comm_1", null)
        )
    }

    @Test
    fun unauthenticated_failsWithIllegalStateException() = runTest {
        seedMasterData()
        val result = repository.getOutstandingDues()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun nonResidentRole_failsWithSecurityException() = runTest {
        seedMasterData()
        sessionManager.saveSession("admin_1", UserRole.ADMIN)

        val result = repository.getOutstandingDues()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun residentWithoutFlat_failsWithIllegalStateException() = runTest {
        seedMasterData()
        sessionManager.saveSession("res_no_flat", UserRole.RESIDENT)

        val result = repository.getOutstandingDues()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun getOutstandingDues_calculatesFromUnpaidAndOverdueBills() = runTest {
        seedMasterData()
        sessionManager.saveSession("res_1", UserRole.RESIDENT)

        val now = System.currentTimeMillis()
        val bills = listOf(
            MaintenanceBillEntity("b1", "flat_1", "comm_1", "October Maint", "Oct 2026", 2500.0, now + 100000, BillStatus.UNPAID, now),
            MaintenanceBillEntity("b2", "flat_1", "comm_1", "September Levy", "Sep 2026", 1200.0, now - 100000, BillStatus.OVERDUE, now),
            MaintenanceBillEntity("b3", "flat_1", "comm_1", "August Maint", "Aug 2026", 2000.0, now - 200000, BillStatus.PAID, now),
            MaintenanceBillEntity("b_other", "flat_2", "comm_1", "Flat 2 Maint", "Oct 2026", 9000.0, now + 100000, BillStatus.UNPAID, now)
        )
        maintenanceDao.insertBills(bills)

        val result = repository.getOutstandingDues()
        assertTrue(result.isSuccess)
        assertEquals(3700.0, result.getOrThrow(), 0.001)
    }

    @Test
    fun getPendingBills_and_getBillHistory_scopesToResidentFlat() = runTest {
        seedMasterData()
        sessionManager.saveSession("res_1", UserRole.RESIDENT)

        val now = System.currentTimeMillis()
        val bills = listOf(
            MaintenanceBillEntity("b1", "flat_1", "comm_1", "Oct Maint", "Oct 2026", 2500.0, now + 100000, BillStatus.UNPAID, now),
            MaintenanceBillEntity("b2", "flat_1", "comm_1", "Sep Levy", "Sep 2026", 1200.0, now - 100000, BillStatus.OVERDUE, now),
            MaintenanceBillEntity("b3", "flat_1", "comm_1", "Aug Maint", "Aug 2026", 2000.0, now - 200000, BillStatus.PAID, now),
            MaintenanceBillEntity("b_other", "flat_2", "comm_1", "Flat 2 Oct", "Oct 2026", 9000.0, now + 100000, BillStatus.UNPAID, now)
        )
        maintenanceDao.insertBills(bills)

        val pending = repository.getPendingBills().getOrThrow()
        assertEquals(2, pending.size)
        assertTrue(pending.any { it.id == "b1" && it.status == BillStatus.UNPAID })
        assertTrue(pending.any { it.id == "b2" && it.status == BillStatus.OVERDUE })
        assertTrue(pending.none { it.id == "b_other" })

        val history = repository.getBillHistory().getOrThrow()
        assertEquals(1, history.size)
        assertEquals("b3", history.first().id)
        assertEquals(BillStatus.PAID, history.first().status)
    }

    @Test
    fun getBillDetails_enforcesFlatOwnership() = runTest {
        seedMasterData()
        val now = System.currentTimeMillis()
        maintenanceDao.insertBills(listOf(
            MaintenanceBillEntity("b1", "flat_1", "comm_1", "Oct Maint", "Oct 2026", 2500.0, now + 100000, BillStatus.UNPAID, now),
            MaintenanceBillEntity("b2", "flat_2", "comm_1", "Flat 2 Oct", "Oct 2026", 3000.0, now + 100000, BillStatus.UNPAID, now)
        ))

        // Resident 1 accessing own bill
        sessionManager.saveSession("res_1", UserRole.RESIDENT)
        val ownDetailRes = repository.getBillDetails("b1")
        assertTrue(ownDetailRes.isSuccess)
        val ownDetail = ownDetailRes.getOrThrow()
        assertEquals("Oct Maint", ownDetail.bill.title)
        assertEquals("A-101", ownDetail.flatNumber)
        assertEquals("Block A", ownDetail.block)
        assertEquals("Greenwood Residency", ownDetail.communityName)
        assertNull(ownDetail.payment)

        // Resident 1 accessing other resident's bill fails
        val otherDetailRes = repository.getBillDetails("b2")
        assertTrue(otherDetailRes.isFailure)
        assertTrue(otherDetailRes.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun payBill_successfullyRecordsPaymentAndUpdatesBillStatus() = runTest {
        seedMasterData()
        sessionManager.saveSession("res_1", UserRole.RESIDENT)

        val now = System.currentTimeMillis()
        maintenanceDao.insertBills(listOf(
            MaintenanceBillEntity("b1", "flat_1", "comm_1", "Oct Maint", "Oct 2026", 2500.0, now + 100000, BillStatus.UNPAID, now)
        ))

        val payResult = repository.payBill("b1", PaymentMethod.UPI_SIMULATED)
        assertTrue(payResult.isSuccess)

        val receipt = payResult.getOrThrow()
        assertEquals(BillStatus.PAID, receipt.bill.status)
        assertEquals(2500.0, receipt.payment.amountPaid, 0.001)
        assertEquals(PaymentMethod.UPI_SIMULATED, receipt.payment.paymentMethod)
        assertTrue(receipt.payment.transactionRef.startsWith("SIM_TXN_"))

        // Check Room state directly
        val updatedBill = maintenanceDao.getBillById("b1")
        assertNotNull(updatedBill)
        assertEquals(BillStatus.PAID, updatedBill?.status)
        assertNotNull(updatedBill?.updatedAt)

        val paymentEntity = maintenanceDao.getPaymentForBill("b1")
        assertNotNull(paymentEntity)
        assertEquals("flat_1", paymentEntity?.flatId)
        assertEquals("res_1", paymentEntity?.residentId)

        // Outstanding dues should now be 0.0
        val remainingDues = repository.getOutstandingDues().getOrThrow()
        assertEquals(0.0, remainingDues, 0.001)
    }

    @Test
    fun payBill_alreadyPaidBill_failsWithIllegalStateException() = runTest {
        seedMasterData()
        sessionManager.saveSession("res_1", UserRole.RESIDENT)

        val now = System.currentTimeMillis()
        maintenanceDao.insertBills(listOf(
            MaintenanceBillEntity("b1", "flat_1", "comm_1", "Oct Maint", "Oct 2026", 2500.0, now - 100000, BillStatus.PAID, now)
        ))

        val payResult = repository.payBill("b1", PaymentMethod.CARD_SIMULATED)
        assertTrue(payResult.isFailure)
        assertTrue(payResult.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun payBill_crossFlatBill_failsWithNoSuchElementException() = runTest {
        seedMasterData()
        sessionManager.saveSession("res_1", UserRole.RESIDENT)

        val now = System.currentTimeMillis()
        maintenanceDao.insertBills(listOf(
            MaintenanceBillEntity("b_other", "flat_2", "comm_1", "Flat 2 Oct", "Oct 2026", 3000.0, now + 100000, BillStatus.UNPAID, now)
        ))

        val payResult = repository.payBill("b_other", PaymentMethod.NET_BANKING_SIMULATED)
        assertTrue(payResult.isFailure)
        assertTrue(payResult.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun getPaymentHistory_scopesToResidentFlat() = runTest {
        seedMasterData()
        sessionManager.saveSession("res_1", UserRole.RESIDENT)

        val now = System.currentTimeMillis()
        maintenanceDao.insertBills(listOf(
            MaintenanceBillEntity("b1", "flat_1", "comm_1", "Oct Maint", "Oct 2026", 2500.0, now, BillStatus.UNPAID, now),
            MaintenanceBillEntity("b2", "flat_2", "comm_1", "Flat 2 Oct", "Oct 2026", 3000.0, now, BillStatus.UNPAID, now)
        ))

        // Resident 1 pays
        repository.payBill("b1", PaymentMethod.UPI_SIMULATED)

        // Resident 2 pays
        sessionManager.saveSession("res_2", UserRole.RESIDENT)
        repository.payBill("b2", PaymentMethod.CASH_SIMULATED)

        // Resident 1 verifies history
        sessionManager.saveSession("res_1", UserRole.RESIDENT)
        val historyRes = repository.getPaymentHistory()
        assertTrue(historyRes.isSuccess)
        val history = historyRes.getOrThrow()
        assertEquals(1, history.size)
        assertEquals("b1", history.first().billId)
        assertEquals(2500.0, history.first().amountPaid, 0.001)
    }
}
