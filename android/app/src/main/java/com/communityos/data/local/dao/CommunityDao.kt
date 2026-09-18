package com.communityos.data.local.dao

import androidx.room.*
import com.communityos.data.local.entity.CommunityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommunityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommunity(community: CommunityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommunities(communities: List<CommunityEntity>)

    @Query("SELECT * FROM communities WHERE id = :id LIMIT 1")
    suspend fun getCommunityById(id: String): CommunityEntity?

    @Query("SELECT * FROM communities WHERE id = :id LIMIT 1")
    fun observeCommunityById(id: String): Flow<CommunityEntity?>

    @Query("SELECT * FROM communities ORDER BY name ASC")
    suspend fun getAllCommunities(): List<CommunityEntity>

    @Query("SELECT * FROM communities ORDER BY name ASC")
    fun observeAllCommunities(): Flow<List<CommunityEntity>>

    @Query("SELECT COUNT(*) FROM communities")
    suspend fun getCount(): Int
}
