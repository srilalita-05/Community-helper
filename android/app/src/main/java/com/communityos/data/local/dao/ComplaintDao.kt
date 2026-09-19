package com.communityos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.communityos.data.local.entity.ComplaintEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ComplaintDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaint(complaint: ComplaintEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaints(complaints: List<ComplaintEntity>)

    @Query("SELECT * FROM complaints WHERE residentId = :residentId ORDER BY createdAt DESC")
    suspend fun getComplaintsForResident(residentId: String): List<ComplaintEntity>

    @Query("SELECT * FROM complaints WHERE residentId = :residentId ORDER BY createdAt DESC")
    fun observeComplaintsForResident(residentId: String): Flow<List<ComplaintEntity>>

    @Query("SELECT * FROM complaints WHERE id = :id AND residentId = :residentId LIMIT 1")
    suspend fun getComplaintByIdAndResident(id: String, residentId: String): ComplaintEntity?

    @Query("SELECT COUNT(*) FROM complaints")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM complaints WHERE residentId = :residentId AND status != 'RESOLVED'")
    suspend fun getPendingComplaintsCount(residentId: String): Int
}
