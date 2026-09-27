package com.communityos.marketplace.event

import com.communityos.marketplace.model.ListingStatus
import com.communityos.marketplace.model.MarketplaceCategory

sealed class CreateEditListingEvent {
    data class OnTitleChanged(val title: String) : CreateEditListingEvent()
    data class OnDescriptionChanged(val description: String) : CreateEditListingEvent()
    data class OnCategoryChanged(val category: MarketplaceCategory) : CreateEditListingEvent()
    data class OnPriceChanged(val price: String) : CreateEditListingEvent()
    data class OnPhoneChanged(val phone: String) : CreateEditListingEvent()
    data class OnStatusChanged(val status: ListingStatus) : CreateEditListingEvent()
    data object Submit : CreateEditListingEvent()
    data object ResetSuccess : CreateEditListingEvent()
}
