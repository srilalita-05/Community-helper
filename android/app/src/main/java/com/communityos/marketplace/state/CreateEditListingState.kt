package com.communityos.marketplace.state

import com.communityos.marketplace.model.ListingStatus
import com.communityos.marketplace.model.MarketplaceCategory

data class CreateEditListingState(
    val listingId: String? = null,
    val title: String = "",
    val description: String = "",
    val category: MarketplaceCategory = MarketplaceCategory.FURNITURE,
    val priceText: String = "",
    val contactPhone: String = "",
    val status: ListingStatus = ListingStatus.ACTIVE,
    val titleError: String? = null,
    val descriptionError: String? = null,
    val priceError: String? = null,
    val phoneError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
) {
    val isEditMode: Boolean get() = listingId != null
}
