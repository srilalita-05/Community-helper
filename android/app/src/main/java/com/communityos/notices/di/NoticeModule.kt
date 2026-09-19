package com.communityos.notices.di

import com.communityos.notices.repository.NoticeRepository
import com.communityos.notices.repository.NoticeRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NoticeModule {

    @Binds
    @Singleton
    abstract fun bindNoticeRepository(
        noticeRepositoryImpl: NoticeRepositoryImpl
    ): NoticeRepository
}
