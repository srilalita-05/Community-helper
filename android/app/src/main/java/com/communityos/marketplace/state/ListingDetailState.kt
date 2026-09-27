package com.communityos.marketplace.state

import com.communityos.marketplace.model.MarketplaceListingDetail

data class ListingDetailState(
    val isLoading: Boolean = true,
    val detail: MarketplaceListingDetail? = null,
    val errorMessage: String? = null,
    val showDeleteDialog: Boolean = false,
    val showSoldDialog: Boolean = false,
    val showCancelDialog: Boolean = false,
    val isDeleted: Boolean = false,
    val actionMessage: String? = null
)
