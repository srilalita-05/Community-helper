package com.communityos.marketplace.event

sealed class ListingDetailEvent {
    data object Refresh : ListingDetailEvent()
    data object ShowDeleteDialog : ListingDetailEvent()
    data object DismissDeleteDialog : ListingDetailEvent()
    data object ConfirmDelete : ListingDetailEvent()
    data object ShowSoldDialog : ListingDetailEvent()
    data object DismissSoldDialog : ListingDetailEvent()
    data object ConfirmMarkSold : ListingDetailEvent()
    data object ShowCancelDialog : ListingDetailEvent()
    data object DismissCancelDialog : ListingDetailEvent()
    data object ConfirmCancelListing : ListingDetailEvent()
    data object DismissMessage : ListingDetailEvent()
}
