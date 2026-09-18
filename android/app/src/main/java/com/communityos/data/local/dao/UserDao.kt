package com.communityos.data.local.dao

import androidx.room.*
import com.communityos.data.local.entity.UserEntity
import com.communityos.models.UserRole
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getUserByPhoneNumber(phoneNumber: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeUserById(id: String): Flow<UserEntity?>

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("UPDATE users SET role = :role WHERE id = :userId")
    suspend fun updateUserRole(userId: String, role: UserRole)

    @Query("UPDATE users SET communityId = :communityId, flatId = :flatId WHERE id = :userId")
    suspend fun updateUserCommunityAndFlat(userId: String, communityId: String?, flatId: String?)

    @Query("UPDATE users SET communityId = :communityId WHERE id = :userId")
    suspend fun updateUserCommunity(userId: String, communityId: String)

    @Query("UPDATE users SET isApproved = :isApproved WHERE id = :userId")
    suspend fun updateApprovalStatus(userId: String, isApproved: Boolean)

    @Query("SELECT * FROM users WHERE communityId = :communityId AND isApproved = 0")
    suspend fun getPendingApprovals(communityId: String): List<UserEntity>
}
