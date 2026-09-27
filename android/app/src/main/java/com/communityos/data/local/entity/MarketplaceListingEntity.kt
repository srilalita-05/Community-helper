package com.communityos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.communityos.marketplace.model.ListingStatus
import com.communityos.marketplace.model.MarketplaceCategory
import com.communityos.marketplace.model.MarketplaceListing

@Entity(
    tableName = "marketplace_listings",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["residentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CommunityEntity::class,
            parentColumns = ["id"],
            childColumns = ["communityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["residentId"]),
        Index(value = ["communityId"]),
        Index(value = ["communityId", "status"]),
        Index(value = ["category"])
    ]
)
data class MarketplaceListingEntity(
    @PrimaryKey val id: String,
    val residentId: String,
    val communityId: String,
    val title: String,
    val description: String,
    val category: MarketplaceCategory,
    val price: Double,
    val contactPhone: String,
    val status: ListingStatus,
    val imageUri: String?,
    val createdAt: Long,
    val updatedAt: Long?
) {
    fun toDomain(): MarketplaceListing {
        return MarketplaceListing(
            id = id,
            residentId = residentId,
            communityId = communityId,
            title = title,
            description = description,
            category = category,
            price = price,
            contactPhone = contactPhone,
            status = status,
            imageUri = imageUri,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(domain: MarketplaceListing): MarketplaceListingEntity {
            return MarketplaceListingEntity(
                id = domain.id,
                residentId = domain.residentId,
                communityId = domain.communityId,
                title = domain.title,
                description = domain.description,
                category = domain.category,
                price = domain.price,
                contactPhone = domain.contactPhone,
                status = domain.status,
                imageUri = domain.imageUri,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt
            )
        }
    }
}
