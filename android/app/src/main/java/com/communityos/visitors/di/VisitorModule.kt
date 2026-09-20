package com.communityos.visitors.di

import com.communityos.visitors.repository.VisitorRepository
import com.communityos.visitors.repository.VisitorRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VisitorModule {

    @Binds
    @Singleton
    abstract fun bindVisitorRepository(
        visitorRepositoryImpl: VisitorRepositoryImpl
    ): VisitorRepository
}
