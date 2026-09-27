package com.communityos.data.local.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.communityos.data.local.CommunityDatabase
import com.communityos.data.local.dao.CommunityDao
import com.communityos.data.local.dao.ComplaintDao
import com.communityos.data.local.dao.FlatDao
import com.communityos.data.local.dao.MarketplaceDao
import com.communityos.data.local.dao.NoticeDao
import com.communityos.data.local.dao.UserDao
import com.communityos.data.local.dao.VisitorDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `notices` (
                    `id` TEXT NOT NULL,
                    `communityId` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `content` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_notices_communityId` ON `notices` (`communityId`)
                """.trimIndent()
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `complaints` (
                    `id` TEXT NOT NULL,
                    `residentId` TEXT NOT NULL,
                    `communityId` TEXT NOT NULL,
                    `flatId` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_complaints_residentId` ON `complaints` (`residentId`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_complaints_communityId` ON `complaints` (`communityId`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_complaints_flatId` ON `complaints` (`flatId`)
                """.trimIndent()
            )
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `visitors` (
                    `id` TEXT NOT NULL,
                    `residentId` TEXT NOT NULL,
                    `communityId` TEXT NOT NULL,
                    `flatId` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `phoneNumber` TEXT NOT NULL,
                    `purpose` TEXT NOT NULL,
                    `vehicleNumber` TEXT,
                    `scheduledArrivalDate` INTEGER NOT NULL,
                    `status` TEXT NOT NULL,
                    `photoUri` TEXT,
                    `checkInTime` INTEGER,
                    `checkOutTime` INTEGER,
                    `verifiedBySecurityId` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_visitors_residentId` ON `visitors` (`residentId`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_visitors_communityId` ON `visitors` (`communityId`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_visitors_flatId` ON `visitors` (`flatId`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_visitors_status` ON `visitors` (`status`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_visitors_communityId_status` ON `visitors` (`communityId`, `status`)
                """.trimIndent()
            )
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `marketplace_listings` (
                    `id` TEXT NOT NULL,
                    `residentId` TEXT NOT NULL,
                    `communityId` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `price` REAL NOT NULL,
                    `contactPhone` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `imageUri` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`residentId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`communityId`) REFERENCES `communities`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_marketplace_listings_residentId` ON `marketplace_listings` (`residentId`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_marketplace_listings_communityId` ON `marketplace_listings` (`communityId`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_marketplace_listings_communityId_status` ON `marketplace_listings` (`communityId`, `status`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_marketplace_listings_category` ON `marketplace_listings` (`category`)
                """.trimIndent()
            )
        }
    }

    @Provides
    @Singleton
    fun provideCommunityDatabase(
        @ApplicationContext context: Context
    ): CommunityDatabase {
        return Room.databaseBuilder(
            context,
            CommunityDatabase::class.java,
            CommunityDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideUserDao(database: CommunityDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideCommunityDao(database: CommunityDatabase): CommunityDao {
        return database.communityDao()
    }

    @Provides
    fun provideFlatDao(database: CommunityDatabase): FlatDao {
        return database.flatDao()
    }

    @Provides
    fun provideNoticeDao(database: CommunityDatabase): NoticeDao {
        return database.noticeDao()
    }

    @Provides
    fun provideComplaintDao(database: CommunityDatabase): ComplaintDao {
        return database.complaintDao()
    }

    @Provides
    fun provideVisitorDao(database: CommunityDatabase): VisitorDao {
        return database.visitorDao()
    }

    @Provides
    fun provideMarketplaceDao(database: CommunityDatabase): MarketplaceDao {
        return database.marketplaceDao()
    }
}
