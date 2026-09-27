package com.communityos.marketplace.model

data class MarketplaceListing(
    val id: String,
    val residentId: String,
    val communityId: String,
    val title: String,
    val description: String,
    val category: MarketplaceCategory,
    val price: Double,
    val contactPhone: String,
    val status: ListingStatus,
    val imageUri: String? = null,
    val createdAt: Long,
    val updatedAt: Long? = null
)
