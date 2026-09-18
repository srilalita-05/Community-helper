package com.communityos.data.local.dao

import androidx.room.*
import com.communityos.data.local.entity.FlatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlat(flat: FlatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlats(flats: List<FlatEntity>)

    @Query("SELECT * FROM flats WHERE id = :id LIMIT 1")
    suspend fun getFlatById(id: String): FlatEntity?

    @Query("SELECT * FROM flats WHERE communityId = :communityId ORDER BY block ASC, flatNumber ASC")
    suspend fun getFlatsForCommunity(communityId: String): List<FlatEntity>

    @Query("SELECT * FROM flats WHERE communityId = :communityId ORDER BY block ASC, flatNumber ASC")
    fun observeFlatsForCommunity(communityId: String): Flow<List<FlatEntity>>

    @Query("SELECT * FROM flats WHERE communityId = :communityId AND flatNumber = :flatNumber LIMIT 1")
    suspend fun getFlatByNumber(communityId: String, flatNumber: String): FlatEntity?

    @Query("SELECT COUNT(*) FROM flats")
    suspend fun getCount(): Int
}
