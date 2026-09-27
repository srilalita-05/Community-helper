package com.communityos.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.communityos.data.local.entity.MarketplaceListingEntity
import com.communityos.marketplace.model.ListingStatus
import com.communityos.marketplace.model.MarketplaceCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketplaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListing(listing: MarketplaceListingEntity)

    @Update
    suspend fun updateListing(listing: MarketplaceListingEntity)

    @Delete
    suspend fun deleteListing(listing: MarketplaceListingEntity)

    @Query("DELETE FROM marketplace_listings WHERE id = :listingId AND residentId = :residentId")
    suspend fun deleteListingByIdAndResident(listingId: String, residentId: String): Int

    @Query("SELECT * FROM marketplace_listings WHERE communityId = :communityId AND status = :activeStatus ORDER BY createdAt DESC")
    suspend fun getActiveListingsForCommunity(
        communityId: String,
        activeStatus: ListingStatus = ListingStatus.ACTIVE
    ): List<MarketplaceListingEntity>

    @Query("SELECT * FROM marketplace_listings WHERE communityId = :communityId AND status = :activeStatus ORDER BY createdAt DESC")
    fun observeActiveListingsForCommunity(
        communityId: String,
        activeStatus: ListingStatus = ListingStatus.ACTIVE
    ): Flow<List<MarketplaceListingEntity>>

    @Query("SELECT * FROM marketplace_listings WHERE communityId = :communityId ORDER BY createdAt DESC")
    suspend fun getListingsForCommunity(communityId: String): List<MarketplaceListingEntity>

    @Query("SELECT * FROM marketplace_listings WHERE communityId = :communityId ORDER BY createdAt DESC")
    fun observeListingsForCommunity(communityId: String): Flow<List<MarketplaceListingEntity>>

    @Query("SELECT * FROM marketplace_listings WHERE communityId = :communityId AND category = :category AND status = :activeStatus ORDER BY createdAt DESC")
    suspend fun getActiveListingsByCategoryForCommunity(
        communityId: String,
        category: MarketplaceCategory,
        activeStatus: ListingStatus = ListingStatus.ACTIVE
    ): List<MarketplaceListingEntity>

    @Query("SELECT * FROM marketplace_listings WHERE communityId = :communityId AND category = :category AND status = :activeStatus ORDER BY createdAt DESC")
    fun observeActiveListingsByCategoryForCommunity(
        communityId: String,
        category: MarketplaceCategory,
        activeStatus: ListingStatus = ListingStatus.ACTIVE
    ): Flow<List<MarketplaceListingEntity>>

    @Query("SELECT * FROM marketplace_listings WHERE id = :listingId AND communityId = :communityId LIMIT 1")
    suspend fun getListingByIdAndCommunity(listingId: String, communityId: String): MarketplaceListingEntity?

    @Query("SELECT * FROM marketplace_listings WHERE residentId = :residentId ORDER BY createdAt DESC")
    suspend fun getListingsForResident(residentId: String): List<MarketplaceListingEntity>

    @Query("SELECT * FROM marketplace_listings WHERE residentId = :residentId ORDER BY createdAt DESC")
    fun observeListingsForResident(residentId: String): Flow<List<MarketplaceListingEntity>>
}
