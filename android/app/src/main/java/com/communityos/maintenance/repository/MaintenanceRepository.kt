package com.communityos.maintenance.repository

import com.communityos.maintenance.model.MaintenanceBill
import com.communityos.maintenance.model.MaintenanceBillDetail
import com.communityos.maintenance.model.MaintenancePayment
import com.communityos.maintenance.model.MaintenancePaymentReceipt
import com.communityos.maintenance.model.PaymentMethod
import kotlinx.coroutines.flow.Flow

interface MaintenanceRepository {
    suspend fun getOutstandingDues(): Result<Double>
    fun observeOutstandingDues(): Flow<Double>

    suspend fun getPendingBills(): Result<List<MaintenanceBill>>
    fun observePendingBills(): Flow<List<MaintenanceBill>>

    suspend fun getBillHistory(): Result<List<MaintenanceBill>>
    fun observeBillHistory(): Flow<List<MaintenanceBill>>

    suspend fun getBillDetails(billId: String): Result<MaintenanceBillDetail>

    suspend fun payBill(billId: String, paymentMethod: PaymentMethod): Result<MaintenancePaymentReceipt>

    suspend fun getPaymentHistory(): Result<List<MaintenancePayment>>
    fun observePaymentHistory(): Flow<List<MaintenancePayment>>
}
