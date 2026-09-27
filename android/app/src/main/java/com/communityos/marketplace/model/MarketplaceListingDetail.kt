package com.communityos.marketplace.model

data class MarketplaceListingDetail(
    val listing: MarketplaceListing,
    val sellerName: String,
    val flatNumber: String?,
    val block: String?,
    val contactPhone: String,
    val isOwner: Boolean
)
