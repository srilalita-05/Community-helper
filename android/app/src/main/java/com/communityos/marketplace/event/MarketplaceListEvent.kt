package com.communityos.marketplace.event

import com.communityos.marketplace.model.MarketplaceCategory
import com.communityos.marketplace.state.MarketplaceTab

sealed class MarketplaceListEvent {
    data object Refresh : MarketplaceListEvent()
    data class SelectTab(val tab: MarketplaceTab) : MarketplaceListEvent()
    data class SelectCategory(val category: MarketplaceCategory) : MarketplaceListEvent()
}
