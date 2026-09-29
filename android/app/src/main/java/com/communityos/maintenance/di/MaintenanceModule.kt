package com.communityos.maintenance.di

import com.communityos.maintenance.repository.MaintenanceRepository
import com.communityos.maintenance.repository.MaintenanceRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MaintenanceModule {

    @Binds
    @Singleton
    abstract fun bindMaintenanceRepository(
        maintenanceRepositoryImpl: MaintenanceRepositoryImpl
    ): MaintenanceRepository
}
