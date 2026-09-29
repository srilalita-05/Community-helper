package com.communityos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.communityos.data.local.converter.RoomConverters
import com.communityos.data.local.dao.CommunityDao
import com.communityos.data.local.dao.ComplaintDao
import com.communityos.data.local.dao.FlatDao
import com.communityos.data.local.dao.MaintenanceDao
import com.communityos.data.local.dao.MarketplaceDao
import com.communityos.data.local.dao.NoticeDao
import com.communityos.data.local.dao.UserDao
import com.communityos.data.local.dao.VisitorDao
import com.communityos.data.local.entity.CommunityEntity
import com.communityos.data.local.entity.ComplaintEntity
import com.communityos.data.local.entity.FlatEntity
import com.communityos.data.local.entity.MaintenanceBillEntity
import com.communityos.data.local.entity.MaintenancePaymentEntity
import com.communityos.data.local.entity.MarketplaceListingEntity
import com.communityos.data.local.entity.NoticeEntity
import com.communityos.data.local.entity.UserEntity
import com.communityos.data.local.entity.VisitorEntity

@Database(
    entities = [
        UserEntity::class,
        CommunityEntity::class,
        FlatEntity::class,
        NoticeEntity::class,
        ComplaintEntity::class,
        VisitorEntity::class,
        MarketplaceListingEntity::class,
        MaintenanceBillEntity::class,
        MaintenancePaymentEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class CommunityDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun communityDao(): CommunityDao
    abstract fun flatDao(): FlatDao
    abstract fun noticeDao(): NoticeDao
    abstract fun complaintDao(): ComplaintDao
    abstract fun visitorDao(): VisitorDao
    abstract fun marketplaceDao(): MarketplaceDao
    abstract fun maintenanceDao(): MaintenanceDao

    companion object {
        const val DATABASE_NAME = "community_os_db"
    }
}
