package com.communityos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.communityos.data.local.entity.NoticeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoticeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotice(notice: NoticeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotices(notices: List<NoticeEntity>)

    @Query("SELECT * FROM notices WHERE communityId = :communityId ORDER BY createdAt DESC")
    suspend fun getNoticesForCommunity(communityId: String): List<NoticeEntity>

    @Query("SELECT * FROM notices WHERE communityId = :communityId ORDER BY createdAt DESC")
    fun observeNoticesForCommunity(communityId: String): Flow<List<NoticeEntity>>

    @Query("SELECT * FROM notices WHERE id = :id AND communityId = :communityId LIMIT 1")
    suspend fun getNoticeByIdAndCommunity(id: String, communityId: String): NoticeEntity?

    @Query("SELECT COUNT(*) FROM notices")
    suspend fun getCount(): Int
}
