package com.communityos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.communityos.data.local.entity.VisitorEntity
import com.communityos.visitors.model.VisitorStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisitor(visitor: VisitorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisitors(visitors: List<VisitorEntity>)

    @Update
    suspend fun updateVisitor(visitor: VisitorEntity)

    // Resident Scoped Queries
    @Query("SELECT * FROM visitors WHERE residentId = :residentId ORDER BY createdAt DESC")
    suspend fun getVisitorsForResident(residentId: String): List<VisitorEntity>

    @Query("SELECT * FROM visitors WHERE residentId = :residentId ORDER BY createdAt DESC")
    fun observeVisitorsForResident(residentId: String): Flow<List<VisitorEntity>>

    @Query("SELECT * FROM visitors WHERE residentId = :residentId AND status = :status ORDER BY createdAt DESC")
    fun observeVisitorsForResidentByStatus(residentId: String, status: VisitorStatus): Flow<List<VisitorEntity>>

    @Query("SELECT * FROM visitors WHERE id = :visitorId AND residentId = :residentId LIMIT 1")
    suspend fun getVisitorByIdAndResident(visitorId: String, residentId: String): VisitorEntity?

    @Query("SELECT COUNT(*) FROM visitors WHERE residentId = :residentId AND status = 'CHECKED_IN'")
    suspend fun countActiveVisitorsForResident(residentId: String): Int

    // Security / Community Scoped Queries
    @Query("SELECT * FROM visitors WHERE communityId = :communityId ORDER BY createdAt DESC")
    suspend fun getVisitorsForCommunity(communityId: String): List<VisitorEntity>

    @Query("SELECT * FROM visitors WHERE communityId = :communityId ORDER BY createdAt DESC")
    fun observeVisitorsForCommunity(communityId: String): Flow<List<VisitorEntity>>

    @Query("SELECT * FROM visitors WHERE communityId = :communityId AND status = :status ORDER BY createdAt DESC")
    fun observeVisitorsForCommunityByStatus(communityId: String, status: VisitorStatus): Flow<List<VisitorEntity>>

    @Query("SELECT * FROM visitors WHERE communityId = :communityId AND status IN (:statuses) ORDER BY createdAt DESC")
    fun observeVisitorsForCommunityByStatuses(communityId: String, statuses: List<VisitorStatus>): Flow<List<VisitorEntity>>

    @Query("SELECT * FROM visitors WHERE id = :visitorId AND communityId = :communityId LIMIT 1")
    suspend fun getVisitorByIdAndCommunity(visitorId: String, communityId: String): VisitorEntity?

    @Query("SELECT COUNT(*) FROM visitors WHERE communityId = :communityId AND status = 'CHECKED_IN'")
    suspend fun countActiveVisitorsForCommunity(communityId: String): Int

    @Query("SELECT COUNT(*) FROM visitors WHERE communityId = :communityId AND status = :status")
    suspend fun countVisitorsForCommunityByStatus(communityId: String, status: VisitorStatus): Int

    @Query("SELECT COUNT(*) FROM visitors")
    suspend fun getCount(): Int
}
