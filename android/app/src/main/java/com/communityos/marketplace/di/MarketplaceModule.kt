package com.communityos.marketplace.di

import com.communityos.marketplace.repository.MarketplaceRepository
import com.communityos.marketplace.repository.MarketplaceRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MarketplaceModule {

    @Binds
    @Singleton
    abstract fun bindMarketplaceRepository(
        marketplaceRepositoryImpl: MarketplaceRepositoryImpl
    ): MarketplaceRepository
}
