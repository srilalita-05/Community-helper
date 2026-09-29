package com.communityos.maintenance.repository

import com.communityos.data.local.dao.CommunityDao
import com.communityos.data.local.dao.FlatDao
import com.communityos.data.local.dao.MaintenanceDao
import com.communityos.data.local.dao.UserDao
import com.communityos.data.local.entity.MaintenancePaymentEntity
import com.communityos.data.local.entity.UserEntity
import com.communityos.data.local.session.SessionManager
import com.communityos.maintenance.model.BillStatus
import com.communityos.maintenance.model.MaintenanceBill
import com.communityos.maintenance.model.MaintenanceBillDetail
import com.communityos.maintenance.model.MaintenancePayment
import com.communityos.maintenance.model.MaintenancePaymentReceipt
import com.communityos.maintenance.model.PaymentMethod
import com.communityos.models.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaintenanceRepositoryImpl @Inject constructor(
    private val maintenanceDao: MaintenanceDao,
    private val userDao: UserDao,
    private val flatDao: FlatDao,
    private val communityDao: CommunityDao,
    private val sessionManager: SessionManager
) : MaintenanceRepository {

    private suspend fun getAuthenticatedResident(): Result<UserEntity> {
        val session = sessionManager.getSession()
            ?: return Result.failure(IllegalStateException("No active session found"))

        val user = userDao.getUserById(session.userId)
            ?: return Result.failure(IllegalStateException("User not found for active session"))

        if (user.role != UserRole.RESIDENT) {
            return Result.failure(SecurityException("Unauthorized: Only residents have access to maintenance"))
        }

        if (user.communityId.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Resident is not associated with a community"))
        }

        if (user.flatId.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Resident is not associated with a flat"))
        }

        return Result.success(user)
    }

    override suspend fun getOutstandingDues(): Result<Double> {
        val residentResult = getAuthenticatedResident()
        if (residentResult.isFailure) {
            return Result.failure(residentResult.exceptionOrNull()!!)
        }
        val resident = residentResult.getOrThrow()
        val total = maintenanceDao.getUnpaidTotalForFlat(resident.flatId!!)
        return Result.success(total)
    }

    override fun observeOutstandingDues(): Flow<Double> = flow {
        val session = sessionManager.getSession() ?: return@flow
        val user = userDao.getUserById(session.userId) ?: return@flow
        if (user.role != UserRole.RESIDENT || user.flatId == null) return@flow

        emitAll(maintenanceDao.observeUnpaidTotalForFlat(user.flatId))
    }

    override suspend fun getPendingBills(): Result<List<MaintenanceBill>> {
        val residentResult = getAuthenticatedResident()
        if (residentResult.isFailure) {
            return Result.failure(residentResult.exceptionOrNull()!!)
        }
        val resident = residentResult.getOrThrow()
        val entities = maintenanceDao.getUnpaidBillsForFlat(resident.flatId!!)
        return Result.success(entities.map { it.toDomain() })
    }

    override fun observePendingBills(): Flow<List<MaintenanceBill>> = flow {
        val session = sessionManager.getSession() ?: return@flow
        val user = userDao.getUserById(session.userId) ?: return@flow
        if (user.role != UserRole.RESIDENT || user.flatId == null) return@flow

        emitAll(maintenanceDao.observeUnpaidBillsForFlat(user.flatId).map { list -> list.map { it.toDomain() } })
    }

    override suspend fun getBillHistory(): Result<List<MaintenanceBill>> {
        val residentResult = getAuthenticatedResident()
        if (residentResult.isFailure) {
            return Result.failure(residentResult.exceptionOrNull()!!)
        }
        val resident = residentResult.getOrThrow()
        val entities = maintenanceDao.getPaidBillsForFlat(resident.flatId!!)
        return Result.success(entities.map { it.toDomain() })
    }

    override fun observeBillHistory(): Flow<List<MaintenanceBill>> = flow {
        val session = sessionManager.getSession() ?: return@flow
        val user = userDao.getUserById(session.userId) ?: return@flow
        if (user.role != UserRole.RESIDENT || user.flatId == null) return@flow

        emitAll(maintenanceDao.observePaidBillsForFlat(user.flatId).map { list -> list.map { it.toDomain() } })
    }

    override suspend fun getBillDetails(billId: String): Result<MaintenanceBillDetail> {
        val residentResult = getAuthenticatedResident()
        if (residentResult.isFailure) {
            return Result.failure(residentResult.exceptionOrNull()!!)
        }
        val resident = residentResult.getOrThrow()

        val billEntity = maintenanceDao.getBillByIdAndFlat(billId, resident.flatId!!)
            ?: return Result.failure(NoSuchElementException("Maintenance bill not found or does not belong to your flat"))

        val paymentEntity = maintenanceDao.getPaymentForBill(billId)
        val flat = flatDao.getFlatById(billEntity.flatId)
        val community = communityDao.getCommunityById(billEntity.communityId)

        val detail = MaintenanceBillDetail(
            bill = billEntity.toDomain(),
            payment = paymentEntity?.toDomain(),
            flatNumber = flat?.flatNumber ?: "",
            block = flat?.block ?: "",
            communityName = community?.name ?: ""
        )
        return Result.success(detail)
    }

    override suspend fun payBill(
        billId: String,
        paymentMethod: PaymentMethod
    ): Result<MaintenancePaymentReceipt> {
        val residentResult = getAuthenticatedResident()
        if (residentResult.isFailure) {
            return Result.failure(residentResult.exceptionOrNull()!!)
        }
        val resident = residentResult.getOrThrow()

        val billEntity = maintenanceDao.getBillByIdAndFlat(billId, resident.flatId!!)
            ?: return Result.failure(NoSuchElementException("Maintenance bill not found or does not belong to your flat"))

        if (!billEntity.toDomain().isPayable) {
            return Result.failure(IllegalStateException("Bill is already paid or not in a payable state"))
        }

        val now = System.currentTimeMillis()
        val paymentId = "pay_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val transactionRef = "SIM_TXN_${now}_${(1000..9999).random()}"

        val paymentEntity = MaintenancePaymentEntity(
            id = paymentId,
            billId = billEntity.id,
            flatId = billEntity.flatId,
            residentId = resident.id,
            amountPaid = billEntity.amount,
            paymentMethod = paymentMethod,
            transactionRef = transactionRef,
            paymentDate = now
        )

        val updatedBillEntity = billEntity.copy(
            status = BillStatus.PAID,
            updatedAt = now
        )

        maintenanceDao.insertPayment(paymentEntity)
        maintenanceDao.updateBill(updatedBillEntity)

        return Result.success(
            MaintenancePaymentReceipt(
                payment = paymentEntity.toDomain(),
                bill = updatedBillEntity.toDomain()
            )
        )
    }

    override suspend fun getPaymentHistory(): Result<List<MaintenancePayment>> {
        val residentResult = getAuthenticatedResident()
        if (residentResult.isFailure) {
            return Result.failure(residentResult.exceptionOrNull()!!)
        }
        val resident = residentResult.getOrThrow()
        val entities = maintenanceDao.getPaymentsForFlat(resident.flatId!!)
        return Result.success(entities.map { it.toDomain() })
    }

    override fun observePaymentHistory(): Flow<List<MaintenancePayment>> = flow {
        val session = sessionManager.getSession() ?: return@flow
        val user = userDao.getUserById(session.userId) ?: return@flow
        if (user.role != UserRole.RESIDENT || user.flatId == null) return@flow

        emitAll(maintenanceDao.observePaymentsForFlat(user.flatId).map { list -> list.map { it.toDomain() } })
    }
}
