package com.communityos.marketplace

import com.communityos.marketplace.model.ListingStatus
import com.communityos.marketplace.model.MarketplaceCategory
import com.communityos.marketplace.model.MarketplaceListing
import com.communityos.marketplace.model.MarketplaceListingDetail
import com.communityos.marketplace.repository.MarketplaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FakeMarketplaceRepository : MarketplaceRepository {

    var communityListingsResult: Result<List<MarketplaceListing>> = Result.success(emptyList())
    var myListingsResult: Result<List<MarketplaceListing>> = Result.success(emptyList())
    var listingDetailsResult: Result<MarketplaceListingDetail> = Result.failure(NoSuchElementException("Not found"))
    var createListingResult: Result<MarketplaceListing>? = null
    var updateListingResult: Result<MarketplaceListing>? = null
    var markSoldResult: Result<MarketplaceListing>? = null
    var cancelListingResult: Result<MarketplaceListing>? = null
    var deleteListingResult: Result<Unit> = Result.success(Unit)

    private val _communityFlow = MutableSharedFlow<List<MarketplaceListing>>(replay = 1)
    private val _myFlow = MutableSharedFlow<List<MarketplaceListing>>(replay = 1)

    override suspend fun getCommunityListings(category: MarketplaceCategory?): Result<List<MarketplaceListing>> {
        return communityListingsResult
    }

    override fun observeCommunityListings(category: MarketplaceCategory?): Flow<List<MarketplaceListing>> {
        return _communityFlow.asSharedFlow()
    }

    override suspend fun getMyListings(): Result<List<MarketplaceListing>> {
        return myListingsResult
    }

    override fun observeMyListings(): Flow<List<MarketplaceListing>> {
        return _myFlow.asSharedFlow()
    }

    override suspend fun getListingDetails(listingId: String): Result<MarketplaceListingDetail> {
        return listingDetailsResult
    }

    override suspend fun createListing(
        title: String,
        description: String,
        category: MarketplaceCategory,
        price: Double,
        contactPhone: String
    ): Result<MarketplaceListing> {
        return createListingResult ?: Result.success(
            MarketplaceListing(
                id = "created_id",
                residentId = "res_1",
                communityId = "comm_1",
                title = title,
                description = description,
                category = category,
                price = price,
                contactPhone = contactPhone,
                status = ListingStatus.ACTIVE,
                imageUri = null,
                createdAt = 1000L,
                updatedAt = null
            )
        )
    }

    override suspend fun updateListing(
        listingId: String,
        title: String,
        description: String,
        category: MarketplaceCategory,
        price: Double,
        contactPhone: String,
        status: ListingStatus
    ): Result<MarketplaceListing> {
        return updateListingResult ?: Result.success(
            MarketplaceListing(
                id = listingId,
                residentId = "res_1",
                communityId = "comm_1",
                title = title,
                description = description,
                category = category,
                price = price,
                contactPhone = contactPhone,
                status = status,
                imageUri = null,
                createdAt = 1000L,
                updatedAt = 2000L
            )
        )
    }

    override suspend fun markListingAsSold(listingId: String): Result<MarketplaceListing> {
        return markSoldResult ?: Result.success(
            MarketplaceListing(
                id = listingId,
                residentId = "res_1",
                communityId = "comm_1",
                title = "Item",
                description = "Desc",
                category = MarketplaceCategory.OTHER,
                price = 100.0,
                contactPhone = "9876543210",
                status = ListingStatus.SOLD,
                imageUri = null,
                createdAt = 1000L,
                updatedAt = 2000L
            )
        )
    }

    override suspend fun cancelListing(listingId: String): Result<MarketplaceListing> {
        return cancelListingResult ?: Result.success(
            MarketplaceListing(
                id = listingId,
                residentId = "res_1",
                communityId = "comm_1",
                title = "Item",
                description = "Desc",
                category = MarketplaceCategory.OTHER,
                price = 100.0,
                contactPhone = "9876543210",
                status = ListingStatus.CANCELLED,
                imageUri = null,
                createdAt = 1000L,
                updatedAt = 2000L
            )
        )
    }

    override suspend fun deleteListing(listingId: String): Result<Unit> {
        return deleteListingResult
    }

    suspend fun emitCommunityListings(listings: List<MarketplaceListing>) {
        _communityFlow.emit(listings)
    }

    suspend fun emitMyListings(listings: List<MarketplaceListing>) {
        _myFlow.emit(listings)
    }
}
