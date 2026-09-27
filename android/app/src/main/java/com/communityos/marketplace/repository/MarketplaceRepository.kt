package com.communityos.marketplace.repository

import com.communityos.marketplace.model.ListingStatus
import com.communityos.marketplace.model.MarketplaceCategory
import com.communityos.marketplace.model.MarketplaceListing
import com.communityos.marketplace.model.MarketplaceListingDetail
import kotlinx.coroutines.flow.Flow

interface MarketplaceRepository {
    suspend fun getCommunityListings(category: MarketplaceCategory? = null): Result<List<MarketplaceListing>>
    fun observeCommunityListings(category: MarketplaceCategory? = null): Flow<List<MarketplaceListing>>
    suspend fun getMyListings(): Result<List<MarketplaceListing>>
    fun observeMyListings(): Flow<List<MarketplaceListing>>
    suspend fun getListingDetails(listingId: String): Result<MarketplaceListingDetail>
    suspend fun createListing(
        title: String,
        description: String,
        category: MarketplaceCategory,
        price: Double,
        contactPhone: String
    ): Result<MarketplaceListing>
    suspend fun updateListing(
        listingId: String,
        title: String,
        description: String,
        category: MarketplaceCategory,
        price: Double,
        contactPhone: String,
        status: ListingStatus
    ): Result<MarketplaceListing>
    suspend fun markListingAsSold(listingId: String): Result<MarketplaceListing>
    suspend fun cancelListing(listingId: String): Result<MarketplaceListing>
    suspend fun deleteListing(listingId: String): Result<Unit>
}
