package com.communityos.marketplace.model

enum class MarketplaceCategory(val displayName: String) {
    ALL("All"),
    FURNITURE("Furniture"),
    ELECTRONICS("Electronics"),
    APPLIANCES("Appliances"),
    VEHICLES("Vehicles"),
    BOOKS("Books"),
    SERVICES("Services"),
    OTHER("Other");

    companion object {
        fun fromString(value: String): MarketplaceCategory {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: OTHER
        }

        val filterCategories: List<MarketplaceCategory> = entries.toList()
        val selectableCategories: List<MarketplaceCategory> = entries.filter { it != ALL }
    }
}
