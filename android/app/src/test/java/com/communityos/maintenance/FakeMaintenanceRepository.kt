package com.communityos.maintenance

import com.communityos.maintenance.model.MaintenanceBill
import com.communityos.maintenance.model.MaintenanceBillDetail
import com.communityos.maintenance.model.MaintenancePayment
import com.communityos.maintenance.model.MaintenancePaymentReceipt
import com.communityos.maintenance.model.PaymentMethod
import com.communityos.maintenance.repository.MaintenanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FakeMaintenanceRepository : MaintenanceRepository {

    var outstandingDuesResult: Result<Double> = Result.success(0.0)
    var pendingBillsResult: Result<List<MaintenanceBill>> = Result.success(emptyList())
    var billHistoryResult: Result<List<MaintenanceBill>> = Result.success(emptyList())
    var billDetailsResult: Result<MaintenanceBillDetail> = Result.failure(NoSuchElementException("Bill not found"))
    var payBillResult: Result<MaintenancePaymentReceipt>? = null
    var paymentHistoryResult: Result<List<MaintenancePayment>> = Result.success(emptyList())

    private val duesFlow = MutableSharedFlow<Double>(replay = 1)
    private val pendingBillsFlow = MutableSharedFlow<List<MaintenanceBill>>(replay = 1)
    private val billHistoryFlow = MutableSharedFlow<List<MaintenanceBill>>(replay = 1)
    private val paymentHistoryFlow = MutableSharedFlow<List<MaintenancePayment>>(replay = 1)

    suspend fun emitDues(dues: Double) = duesFlow.emit(dues)
    suspend fun emitPendingBills(bills: List<MaintenanceBill>) = pendingBillsFlow.emit(bills)

    override suspend fun getOutstandingDues(): Result<Double> = outstandingDuesResult

    override fun observeOutstandingDues(): Flow<Double> = duesFlow.asSharedFlow()

    override suspend fun getPendingBills(): Result<List<MaintenanceBill>> = pendingBillsResult

    override fun observePendingBills(): Flow<List<MaintenanceBill>> = pendingBillsFlow.asSharedFlow()

    override suspend fun getBillHistory(): Result<List<MaintenanceBill>> = billHistoryResult

    override fun observeBillHistory(): Flow<List<MaintenanceBill>> = billHistoryFlow.asSharedFlow()

    override suspend fun getBillDetails(billId: String): Result<MaintenanceBillDetail> = billDetailsResult

    override suspend fun payBill(
        billId: String,
        paymentMethod: PaymentMethod
    ): Result<MaintenancePaymentReceipt> {
        return payBillResult ?: Result.failure(IllegalStateException("No payBill result configured"))
    }

    override suspend fun getPaymentHistory(): Result<List<MaintenancePayment>> = paymentHistoryResult

    override fun observePaymentHistory(): Flow<List<MaintenancePayment>> = paymentHistoryFlow.asSharedFlow()
}
