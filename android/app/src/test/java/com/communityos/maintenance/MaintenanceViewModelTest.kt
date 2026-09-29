package com.communityos.maintenance

import androidx.lifecycle.SavedStateHandle
import com.communityos.maintenance.domain.GetBillDetailsUseCase
import com.communityos.maintenance.domain.GetBillHistoryUseCase
import com.communityos.maintenance.domain.GetPaymentHistoryUseCase
import com.communityos.maintenance.domain.GetPendingDuesUseCase
import com.communityos.maintenance.domain.PayMaintenanceBillUseCase
import com.communityos.maintenance.event.BillDetailsEvent
import com.communityos.maintenance.event.MaintenanceDashboardEvent
import com.communityos.maintenance.event.PaymentConfirmationEvent
import com.communityos.maintenance.event.PaymentEffect
import com.communityos.maintenance.model.BillStatus
import com.communityos.maintenance.model.MaintenanceBill
import com.communityos.maintenance.model.MaintenanceBillDetail
import com.communityos.maintenance.model.MaintenancePayment
import com.communityos.maintenance.model.MaintenancePaymentReceipt
import com.communityos.maintenance.model.PaymentMethod
import com.communityos.maintenance.state.MaintenanceTab
import com.communityos.maintenance.viewmodel.BillDetailsViewModel
import com.communityos.maintenance.viewmodel.MaintenanceDashboardViewModel
import com.communityos.maintenance.viewmodel.PaymentConfirmationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MaintenanceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeMaintenanceRepository

    private lateinit var getPendingDuesUseCase: GetPendingDuesUseCase
    private lateinit var getBillHistoryUseCase: GetBillHistoryUseCase
    private lateinit var getPaymentHistoryUseCase: GetPaymentHistoryUseCase
    private lateinit var getBillDetailsUseCase: GetBillDetailsUseCase
    private lateinit var payMaintenanceBillUseCase: PayMaintenanceBillUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeMaintenanceRepository()
        getPendingDuesUseCase = GetPendingDuesUseCase(fakeRepository)
        getBillHistoryUseCase = GetBillHistoryUseCase(fakeRepository)
        getPaymentHistoryUseCase = GetPaymentHistoryUseCase(fakeRepository)
        getBillDetailsUseCase = GetBillDetailsUseCase(fakeRepository)
        payMaintenanceBillUseCase = PayMaintenanceBillUseCase(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // MaintenanceDashboardViewModel Tests
    // =========================================================================

    @Test
    fun dashboardViewModel_loadData_populatesState() = runTest(testDispatcher) {
        val now = System.currentTimeMillis()
        val pending = listOf(
            MaintenanceBill("b1", "f1", "c1", "October Maint", "Oct 2026", 2500.0, now + 100000, BillStatus.UNPAID, now, null)
        )
        val history = listOf(
            MaintenanceBill("b2", "f1", "c1", "August Maint", "Aug 2026", 2000.0, now - 200000, BillStatus.PAID, now, now)
        )
        val payments = listOf(
            MaintenancePayment("p1", "b2", "f1", "u1", 2000.0, PaymentMethod.UPI_SIMULATED, "SIM_001", now)
        )

        fakeRepository.outstandingDuesResult = Result.success(2500.0)
        fakeRepository.pendingBillsResult = Result.success(pending)
        fakeRepository.billHistoryResult = Result.success(history)
        fakeRepository.paymentHistoryResult = Result.success(payments)

        val viewModel = MaintenanceDashboardViewModel(
            getPendingDuesUseCase,
            getBillHistoryUseCase,
            getPaymentHistoryUseCase
        )
        testScheduler.advanceUntilIdle()

        viewModel.loadData()
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(2500.0, state.outstandingTotal, 0.001)
        assertEquals(1, state.pendingBills.size)
        assertEquals(1, state.billHistory.size)
        assertEquals(1, state.payments.size)
    }

    @Test
    fun dashboardViewModel_tabSelection_updatesState() = runTest(testDispatcher) {
        val viewModel = MaintenanceDashboardViewModel(
            getPendingDuesUseCase,
            getBillHistoryUseCase,
            getPaymentHistoryUseCase
        )
        testScheduler.advanceUntilIdle()

        assertEquals(MaintenanceTab.PENDING, viewModel.state.value.selectedTab)

        viewModel.onEvent(MaintenanceDashboardEvent.SelectTab(MaintenanceTab.HISTORY))
        assertEquals(MaintenanceTab.HISTORY, viewModel.state.value.selectedTab)

        viewModel.onEvent(MaintenanceDashboardEvent.SelectTab(MaintenanceTab.PAYMENTS))
        assertEquals(MaintenanceTab.PAYMENTS, viewModel.state.value.selectedTab)
    }

    @Test
    fun dashboardViewModel_errorHandling_and_dismiss() = runTest(testDispatcher) {
        fakeRepository.outstandingDuesResult = Result.failure(RuntimeException("Network error"))
        fakeRepository.pendingBillsResult = Result.failure(RuntimeException("Network error"))

        val viewModel = MaintenanceDashboardViewModel(
            getPendingDuesUseCase,
            getBillHistoryUseCase,
            getPaymentHistoryUseCase
        )
        viewModel.loadData()
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertNotNull(state.error)
        assertEquals("Network error", state.error)

        viewModel.onEvent(MaintenanceDashboardEvent.DismissError)
        assertNull(viewModel.state.value.error)
    }

    // =========================================================================
    // BillDetailsViewModel Tests
    // =========================================================================

    @Test
    fun billDetailsViewModel_loadSuccess_updatesState() = runTest(testDispatcher) {
        val now = System.currentTimeMillis()
        val bill = MaintenanceBill("b1", "f1", "c1", "October Maint", "Oct 2026", 2500.0, now + 100000, BillStatus.UNPAID, now, null)
        val detail = MaintenanceBillDetail(bill, null, "A-101", "Block A", "Greenwood")

        fakeRepository.billDetailsResult = Result.success(detail)

        val handle = SavedStateHandle(mapOf("billId" to "b1"))
        val viewModel = BillDetailsViewModel(getBillDetailsUseCase, handle)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNotNull(state.billDetail)
        assertEquals("October Maint", state.billDetail?.bill?.title)
        assertEquals("A-101", state.billDetail?.flatNumber)
    }

    @Test
    fun billDetailsViewModel_loadFailure_updatesError() = runTest(testDispatcher) {
        fakeRepository.billDetailsResult = Result.failure(NoSuchElementException("Bill not found"))

        val handle = SavedStateHandle(mapOf("billId" to "b_unknown"))
        val viewModel = BillDetailsViewModel(getBillDetailsUseCase, handle)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.billDetail)
        assertEquals("Bill not found", state.error)

        viewModel.onEvent(BillDetailsEvent.DismissError)
        assertNull(viewModel.state.value.error)
    }

    // =========================================================================
    // PaymentConfirmationViewModel Tests
    // =========================================================================

    @Test
    fun paymentConfirmationViewModel_loadBill_success() = runTest(testDispatcher) {
        val now = System.currentTimeMillis()
        val bill = MaintenanceBill("b1", "f1", "c1", "October Maint", "Oct 2026", 2500.0, now + 100000, BillStatus.UNPAID, now, null)
        val detail = MaintenanceBillDetail(bill, null, "A-101", "Block A", "Greenwood")

        fakeRepository.billDetailsResult = Result.success(detail)

        val handle = SavedStateHandle(mapOf("billId" to "b1"))
        val viewModel = PaymentConfirmationViewModel(getBillDetailsUseCase, payMaintenanceBillUseCase, handle)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("b1", state.bill?.id)
        assertEquals(PaymentMethod.UPI_SIMULATED, state.selectedMethod)
    }

    @Test
    fun paymentConfirmationViewModel_selectMethod_updatesSelection() = runTest(testDispatcher) {
        val handle = SavedStateHandle(mapOf("billId" to "b1"))
        val viewModel = PaymentConfirmationViewModel(getBillDetailsUseCase, payMaintenanceBillUseCase, handle)
        testScheduler.advanceUntilIdle()

        viewModel.onEvent(PaymentConfirmationEvent.SelectMethod(PaymentMethod.CARD_SIMULATED))
        assertEquals(PaymentMethod.CARD_SIMULATED, viewModel.state.value.selectedMethod)

        viewModel.onEvent(PaymentConfirmationEvent.SelectMethod(PaymentMethod.NET_BANKING_SIMULATED))
        assertEquals(PaymentMethod.NET_BANKING_SIMULATED, viewModel.state.value.selectedMethod)
    }

    @Test
    fun paymentConfirmationViewModel_confirmPayment_success_updatesReceiptAndEmitsEffect() = runTest(testDispatcher) {
        val now = System.currentTimeMillis()
        val unpaidBill = MaintenanceBill("b1", "f1", "c1", "October Maint", "Oct 2026", 2500.0, now + 100000, BillStatus.UNPAID, now, null)
        val paidBill = unpaidBill.copy(status = BillStatus.PAID, updatedAt = now)
        val detail = MaintenanceBillDetail(unpaidBill, null, "A-101", "Block A", "Greenwood")
        val payment = MaintenancePayment("p1", "b1", "f1", "u1", 2500.0, PaymentMethod.UPI_SIMULATED, "SIM_TXN_999", now)
        val receipt = MaintenancePaymentReceipt(payment, paidBill)

        fakeRepository.billDetailsResult = Result.success(detail)
        fakeRepository.payBillResult = Result.success(receipt)

        val handle = SavedStateHandle(mapOf("billId" to "b1"))
        val viewModel = PaymentConfirmationViewModel(getBillDetailsUseCase, payMaintenanceBillUseCase, handle)
        testScheduler.advanceUntilIdle()

        var receivedEffect: PaymentEffect? = null
        val job = launch {
            viewModel.effect.collect { receivedEffect = it }
        }

        viewModel.onEvent(PaymentConfirmationEvent.ConfirmPayment)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isProcessing)
        assertTrue(state.isSuccess)
        assertNotNull(state.receipt)
        assertEquals("SIM_TXN_999", state.receipt?.payment?.transactionRef)
        assertEquals(BillStatus.PAID, state.receipt?.bill?.status)

        assertNotNull(receivedEffect)
        assertTrue(receivedEffect is PaymentEffect.PaymentSuccess)
        assertEquals("SIM_TXN_999", (receivedEffect as PaymentEffect.PaymentSuccess).transactionRef)

        job.cancel()
    }

    @Test
    fun paymentConfirmationViewModel_confirmPayment_failure_updatesError() = runTest(testDispatcher) {
        val now = System.currentTimeMillis()
        val unpaidBill = MaintenanceBill("b1", "f1", "c1", "October Maint", "Oct 2026", 2500.0, now + 100000, BillStatus.UNPAID, now, null)
        val detail = MaintenanceBillDetail(unpaidBill, null, "A-101", "Block A", "Greenwood")

        fakeRepository.billDetailsResult = Result.success(detail)
        fakeRepository.payBillResult = Result.failure(IllegalStateException("Card declined in simulation"))

        val handle = SavedStateHandle(mapOf("billId" to "b1"))
        val viewModel = PaymentConfirmationViewModel(getBillDetailsUseCase, payMaintenanceBillUseCase, handle)
        testScheduler.advanceUntilIdle()

        viewModel.onEvent(PaymentConfirmationEvent.ConfirmPayment)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isProcessing)
        assertFalse(state.isSuccess)
        assertEquals("Card declined in simulation", state.error)

        viewModel.onEvent(PaymentConfirmationEvent.DismissError)
        assertNull(viewModel.state.value.error)
    }
}
