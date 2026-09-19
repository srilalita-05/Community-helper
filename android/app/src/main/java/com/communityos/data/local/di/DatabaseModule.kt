package com.communityos.data.local.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.communityos.data.local.CommunityDatabase
import com.communityos.data.local.dao.CommunityDao
import com.communityos.data.local.dao.FlatDao
import com.communityos.data.local.dao.NoticeDao
import com.communityos.data.local.dao.UserDao
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
            .addMigrations(MIGRATION_1_2)
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
}
