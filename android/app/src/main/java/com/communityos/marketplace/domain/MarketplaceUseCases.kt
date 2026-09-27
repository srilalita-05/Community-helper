package com.communityos.marketplace.domain

import com.communityos.marketplace.model.ListingStatus
import com.communityos.marketplace.model.MarketplaceCategory
import com.communityos.marketplace.model.MarketplaceListing
import com.communityos.marketplace.model.MarketplaceListingDetail
import com.communityos.marketplace.repository.MarketplaceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCommunityListingsUseCase @Inject constructor(
    private val repository: MarketplaceRepository
) {
    suspend operator fun invoke(category: MarketplaceCategory? = null): Result<List<MarketplaceListing>> {
        return repository.getCommunityListings(category)
    }

    fun observe(category: MarketplaceCategory? = null): Flow<List<MarketplaceListing>> {
        return repository.observeCommunityListings(category)
    }
}

class GetMyListingsUseCase @Inject constructor(
    private val repository: MarketplaceRepository
) {
    suspend operator fun invoke(): Result<List<MarketplaceListing>> {
        return repository.getMyListings()
    }

    fun observe(): Flow<List<MarketplaceListing>> {
        return repository.observeMyListings()
    }
}

class GetListingDetailsUseCase @Inject constructor(
    private val repository: MarketplaceRepository
) {
    suspend operator fun invoke(listingId: String): Result<MarketplaceListingDetail> {
        return repository.getListingDetails(listingId)
    }
}

class CreateListingUseCase @Inject constructor(
    private val repository: MarketplaceRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String,
        category: MarketplaceCategory,
        price: Double,
        contactPhone: String
    ): Result<MarketplaceListing> {
        return repository.createListing(
            title = title,
            description = description,
            category = category,
            price = price,
            contactPhone = contactPhone
        )
    }
}

class UpdateListingUseCase @Inject constructor(
    private val repository: MarketplaceRepository
) {
    suspend operator fun invoke(
        listingId: String,
        title: String,
        description: String,
        category: MarketplaceCategory,
        price: Double,
        contactPhone: String,
        status: ListingStatus
    ): Result<MarketplaceListing> {
        return repository.updateListing(
            listingId = listingId,
            title = title,
            description = description,
            category = category,
            price = price,
            contactPhone = contactPhone,
            status = status
        )
    }

    suspend fun markAsSold(listingId: String): Result<MarketplaceListing> {
        return repository.markListingAsSold(listingId)
    }

    suspend fun cancel(listingId: String): Result<MarketplaceListing> {
        return repository.cancelListing(listingId)
    }
}

class DeleteListingUseCase @Inject constructor(
    private val repository: MarketplaceRepository
) {
    suspend operator fun invoke(listingId: String): Result<Unit> {
        return repository.deleteListing(listingId)
    }
}
