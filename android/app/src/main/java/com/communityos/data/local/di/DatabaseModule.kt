package com.communityos.data.local.di

import android.content.Context
import androidx.room.Room
import com.communityos.data.local.CommunityDatabase
import com.communityos.data.local.dao.CommunityDao
import com.communityos.data.local.dao.FlatDao
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
}
