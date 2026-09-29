package com.communityos.data.local

import android.content.Context
import com.communityos.data.local.dao.CommunityDao
import com.communityos.data.local.dao.FlatDao
import com.communityos.data.local.dao.NoticeDao
import com.communityos.data.local.entity.CommunityEntity
import com.communityos.data.local.entity.FlatEntity
import com.communityos.data.local.entity.NoticeEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Idempotent initializer for developer-managed master community seed data and notices.
 *
 * Loads initial communities, blocks, and flats from assets/community_data.json
 * and initial notices from assets/notice_data.json into Room persistence.
 * Never wipes existing user or session data.
 */
@Singleton
class DatabaseInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val communityDao: CommunityDao,
    private val flatDao: FlatDao,
    private val noticeDao: NoticeDao,
    private val maintenanceDao: com.communityos.data.local.dao.MaintenanceDao
) {

    suspend fun seedDemoDataIfEmpty() = withContext(Dispatchers.IO) {
        val jsonString = try {
            context.assets.open(COMMUNITY_DATA_ASSET).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            // Fallback for JVM/Robolectric test environments where assets are on disk
            val fileCandidates = listOf(
                File("src/main/assets/$COMMUNITY_DATA_ASSET"),
                File("app/src/main/assets/$COMMUNITY_DATA_ASSET"),
                File("../app/src/main/assets/$COMMUNITY_DATA_ASSET")
            )
            val fallbackFile = fileCandidates.firstOrNull { it.exists() }
            fallbackFile?.readText() ?: return@withContext
        }
        seedFromJson(jsonString)

        val noticeJsonString = try {
            context.assets.open(NOTICE_DATA_ASSET).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            val fileCandidates = listOf(
                File("src/main/assets/$NOTICE_DATA_ASSET"),
                File("app/src/main/assets/$NOTICE_DATA_ASSET"),
                File("../app/src/main/assets/$NOTICE_DATA_ASSET")
            )
            val fallbackFile = fileCandidates.firstOrNull { it.exists() }
            fallbackFile?.readText()
        }
        if (noticeJsonString != null) {
            seedNoticesFromJson(noticeJsonString)
        }

        seedMaintenanceDataIfEmpty()
    }

    suspend fun seedMaintenanceDataIfEmpty() = withContext(Dispatchers.IO) {
        if (maintenanceDao.getBillCount() == 0) {
            val now = System.currentTimeMillis()
            val bills = listOf(
                com.communityos.data.local.entity.MaintenanceBillEntity(
                    id = "bill_b304_current",
                    flatId = "flat_b304",
                    communityId = "community_orchard_heights",
                    title = "Monthly Maintenance — October 2026",
                    period = "October 2026",
                    amount = 2500.00,
                    dueDate = now + (14L * 24 * 60 * 60 * 1000), // Due in 14 days
                    status = com.communityos.maintenance.model.BillStatus.UNPAID,
                    createdAt = now - (5L * 24 * 60 * 60 * 1000),
                    updatedAt = null
                ),
                com.communityos.data.local.entity.MaintenanceBillEntity(
                    id = "bill_b304_overdue",
                    flatId = "flat_b304",
                    communityId = "community_orchard_heights",
                    title = "Clubhouse & Gym Levy — September 2026",
                    period = "September 2026",
                    amount = 1200.00,
                    dueDate = now - (10L * 24 * 60 * 60 * 1000), // Overdue by 10 days
                    status = com.communityos.maintenance.model.BillStatus.OVERDUE,
                    createdAt = now - (35L * 24 * 60 * 60 * 1000),
                    updatedAt = null
                ),
                com.communityos.data.local.entity.MaintenanceBillEntity(
                    id = "bill_b304_paid_aug",
                    flatId = "flat_b304",
                    communityId = "community_orchard_heights",
                    title = "Monthly Maintenance — August 2026",
                    period = "August 2026",
                    amount = 2500.00,
                    dueDate = now - (45L * 24 * 60 * 60 * 1000),
                    status = com.communityos.maintenance.model.BillStatus.PAID,
                    createdAt = now - (65L * 24 * 60 * 60 * 1000),
                    updatedAt = now - (48L * 24 * 60 * 60 * 1000)
                ),
                com.communityos.data.local.entity.MaintenanceBillEntity(
                    id = "bill_a101_current",
                    flatId = "flat_a101",
                    communityId = "community_orchard_heights",
                    title = "Monthly Maintenance — October 2026",
                    period = "October 2026",
                    amount = 2200.00,
                    dueDate = now + (14L * 24 * 60 * 60 * 1000),
                    status = com.communityos.maintenance.model.BillStatus.UNPAID,
                    createdAt = now - (5L * 24 * 60 * 60 * 1000),
                    updatedAt = null
                )
            )

            val payments = listOf(
                com.communityos.data.local.entity.MaintenancePaymentEntity(
                    id = "pay_b304_aug_001",
                    billId = "bill_b304_paid_aug",
                    flatId = "flat_b304",
                    residentId = "user_resident_1",
                    amountPaid = 2500.00,
                    paymentMethod = com.communityos.maintenance.model.PaymentMethod.UPI_SIMULATED,
                    transactionRef = "SIM_TXN_AUG_2026_98214",
                    paymentDate = now - (48L * 24 * 60 * 60 * 1000)
                )
            )

            maintenanceDao.insertBills(bills)
            maintenanceDao.insertPayments(payments)
        }
    }

    suspend fun seedFromJson(jsonString: String) = withContext(Dispatchers.IO) {
        val jsonObject = JSONObject(jsonString)
        val communitiesArray = jsonObject.optJSONArray("communities") ?: return@withContext

        val communitiesToInsert = mutableListOf<CommunityEntity>()
        val flatsToInsert = mutableListOf<FlatEntity>()

        for (i in 0 until communitiesArray.length()) {
            val commObj = communitiesArray.getJSONObject(i)
            val communityId = commObj.getString("id")
            val communityName = commObj.getString("name")
            val address = commObj.optString("address", "")
            val city = commObj.optString("city", "")

            val blocksArray = commObj.optJSONArray("blocks")
            val totalBlocks = blocksArray?.length() ?: 0

            val communityEntity = CommunityEntity(
                id = communityId,
                name = communityName,
                address = address,
                city = city,
                totalBlocks = totalBlocks
            )
            communitiesToInsert.add(communityEntity)

            if (blocksArray != null) {
                for (b in 0 until blocksArray.length()) {
                    val blockObj = blocksArray.getJSONObject(b)
                    val blockId = blockObj.optString("id", "")
                    val blockName = blockObj.optString("name", blockId)
                    val flatsArray = blockObj.optJSONArray("flats")

                    if (flatsArray != null) {
                        for (f in 0 until flatsArray.length()) {
                            val flatObj = flatsArray.getJSONObject(f)
                            val flatNumber = flatObj.optString("number").ifEmpty {
                                flatObj.optString("flatNumber")
                            }
                            val flatId = flatObj.optString("id").ifEmpty {
                                "${communityId}_${blockId}_${flatNumber.replace("-", "").lowercase()}"
                            }
                            val floor = flatObj.optInt("floor", 1)

                            val flatEntity = FlatEntity(
                                id = flatId,
                                communityId = communityId,
                                block = blockName,
                                flatNumber = flatNumber,
                                floor = floor
                            )
                            flatsToInsert.add(flatEntity)
                        }
                    }
                }
            }
        }

        // Idempotent insertion:
        if (communityDao.getCount() == 0) {
            communityDao.insertCommunities(communitiesToInsert)
        } else {
            for (community in communitiesToInsert) {
                if (communityDao.getCommunityById(community.id) == null) {
                    communityDao.insertCommunity(community)
                }
            }
        }

        if (flatDao.getCount() == 0) {
            flatDao.insertFlats(flatsToInsert)
        } else {
            for (flat in flatsToInsert) {
                if (flatDao.getFlatByNumber(flat.communityId, flat.flatNumber) == null) {
                    flatDao.insertFlat(flat)
                }
            }
        }
    }

    suspend fun seedNoticesFromJson(jsonString: String) = withContext(Dispatchers.IO) {
        val jsonObject = JSONObject(jsonString)
        val noticesArray = jsonObject.optJSONArray("notices") ?: return@withContext

        val noticesToInsert = mutableListOf<NoticeEntity>()
        for (i in 0 until noticesArray.length()) {
            val noticeObj = noticesArray.getJSONObject(i)
            val id = noticeObj.getString("id")
            val communityId = noticeObj.getString("communityId")
            val title = noticeObj.getString("title")
            val content = noticeObj.getString("content")
            val createdAt = noticeObj.getLong("createdAt")
            val updatedAt = if (noticeObj.has("updatedAt") && !noticeObj.isNull("updatedAt")) {
                noticeObj.getLong("updatedAt")
            } else {
                null
            }

            noticesToInsert.add(
                NoticeEntity(
                    id = id,
                    communityId = communityId,
                    title = title,
                    content = content,
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )
            )
        }

        if (noticeDao.getCount() == 0) {
            noticeDao.insertNotices(noticesToInsert)
        } else {
            for (notice in noticesToInsert) {
                if (noticeDao.getNoticeByIdAndCommunity(notice.id, notice.communityId) == null) {
                    noticeDao.insertNotice(notice)
                }
            }
        }
    }

    companion object {
        const val COMMUNITY_DATA_ASSET = "community_data.json"
        const val NOTICE_DATA_ASSET = "notice_data.json"
    }
}
