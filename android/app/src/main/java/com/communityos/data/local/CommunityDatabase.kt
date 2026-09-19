package com.communityos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.communityos.data.local.converter.RoomConverters
import com.communityos.data.local.dao.CommunityDao
import com.communityos.data.local.dao.FlatDao
import com.communityos.data.local.dao.UserDao
import com.communityos.data.local.dao.NoticeDao
import com.communityos.data.local.entity.CommunityEntity
import com.communityos.data.local.entity.FlatEntity
import com.communityos.data.local.entity.NoticeEntity
import com.communityos.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        CommunityEntity::class,
        FlatEntity::class,
        NoticeEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class CommunityDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun communityDao(): CommunityDao
    abstract fun flatDao(): FlatDao
    abstract fun noticeDao(): NoticeDao

    companion object {
        const val DATABASE_NAME = "community_os_db"
    }
}
