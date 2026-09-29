package com.communityos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.communityos.data.local.entity.MaintenanceBillEntity
import com.communityos.data.local.entity.MaintenancePaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: MaintenanceBillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBills(bills: List<MaintenanceBillEntity>)

    @Update
    suspend fun updateBill(bill: MaintenanceBillEntity)

    @Query("SELECT * FROM maintenance_bills WHERE flatId = :flatId ORDER BY dueDate DESC")
    suspend fun getBillsForFlat(flatId: String): List<MaintenanceBillEntity>

    @Query("SELECT * FROM maintenance_bills WHERE flatId = :flatId ORDER BY dueDate DESC")
    fun observeBillsForFlat(flatId: String): Flow<List<MaintenanceBillEntity>>

    @Query("SELECT * FROM maintenance_bills WHERE flatId = :flatId AND status IN ('UNPAID', 'OVERDUE') ORDER BY dueDate ASC")
    suspend fun getUnpaidBillsForFlat(flatId: String): List<MaintenanceBillEntity>

    @Query("SELECT * FROM maintenance_bills WHERE flatId = :flatId AND status IN ('UNPAID', 'OVERDUE') ORDER BY dueDate ASC")
    fun observeUnpaidBillsForFlat(flatId: String): Flow<List<MaintenanceBillEntity>>

    @Query("SELECT * FROM maintenance_bills WHERE flatId = :flatId AND status = 'PAID' ORDER BY dueDate DESC")
    suspend fun getPaidBillsForFlat(flatId: String): List<MaintenanceBillEntity>

    @Query("SELECT * FROM maintenance_bills WHERE flatId = :flatId AND status = 'PAID' ORDER BY dueDate DESC")
    fun observePaidBillsForFlat(flatId: String): Flow<List<MaintenanceBillEntity>>

    @Query("SELECT * FROM maintenance_bills WHERE id = :billId LIMIT 1")
    suspend fun getBillById(billId: String): MaintenanceBillEntity?

    @Query("SELECT * FROM maintenance_bills WHERE id = :billId AND flatId = :flatId LIMIT 1")
    suspend fun getBillByIdAndFlat(billId: String, flatId: String): MaintenanceBillEntity?

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM maintenance_bills WHERE flatId = :flatId AND status IN ('UNPAID', 'OVERDUE')")
    suspend fun getUnpaidTotalForFlat(flatId: String): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM maintenance_bills WHERE flatId = :flatId AND status IN ('UNPAID', 'OVERDUE')")
    fun observeUnpaidTotalForFlat(flatId: String): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: MaintenancePaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<MaintenancePaymentEntity>)

    @Query("SELECT * FROM maintenance_payments WHERE flatId = :flatId ORDER BY paymentDate DESC")
    suspend fun getPaymentsForFlat(flatId: String): List<MaintenancePaymentEntity>

    @Query("SELECT * FROM maintenance_payments WHERE flatId = :flatId ORDER BY paymentDate DESC")
    fun observePaymentsForFlat(flatId: String): Flow<List<MaintenancePaymentEntity>>

    @Query("SELECT * FROM maintenance_payments WHERE billId = :billId LIMIT 1")
    suspend fun getPaymentForBill(billId: String): MaintenancePaymentEntity?

    @Query("SELECT COUNT(*) FROM maintenance_bills")
    suspend fun getBillCount(): Int

    @Query("SELECT COUNT(*) FROM maintenance_payments")
    suspend fun getPaymentCount(): Int
}
