package com.communityos.marketplace.state

import com.communityos.marketplace.model.MarketplaceCategory
import com.communityos.marketplace.model.MarketplaceListing

enum class MarketplaceTab(val title: String) {
    ALL_LISTINGS("All Listings"),
    MY_LISTINGS("My Listings")
}

data class MarketplaceListState(
    val isLoading: Boolean = true,
    val communityListings: List<MarketplaceListing> = emptyList(),
    val myListings: List<MarketplaceListing> = emptyList(),
    val selectedTab: MarketplaceTab = MarketplaceTab.ALL_LISTINGS,
    val selectedCategory: MarketplaceCategory = MarketplaceCategory.ALL,
    val errorMessage: String? = null
) {
    val displayedListings: List<MarketplaceListing>
        get() {
            val baseList = when (selectedTab) {
                MarketplaceTab.ALL_LISTINGS -> communityListings
                MarketplaceTab.MY_LISTINGS -> myListings
            }
            return if (selectedCategory == MarketplaceCategory.ALL) {
                baseList
            } else {
                baseList.filter { it.category == selectedCategory }
            }
        }
}
