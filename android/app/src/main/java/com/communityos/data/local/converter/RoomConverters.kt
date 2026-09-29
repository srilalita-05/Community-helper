package com.communityos.data.local.converter

import androidx.room.TypeConverter
import com.communityos.complaints.model.ComplaintStatus
import com.communityos.models.UserRole

class RoomConverters {

    @TypeConverter
    fun fromUserRole(role: UserRole): String {
        return role.name
    }

    @TypeConverter
    fun toUserRole(value: String?): UserRole {
        return UserRole.fromString(value)
    }

    @TypeConverter
    fun fromComplaintStatus(status: ComplaintStatus): String {
        return status.name
    }

    @TypeConverter
    fun toComplaintStatus(value: String?): ComplaintStatus {
        return ComplaintStatus.fromString(value)
    }

    @TypeConverter
    fun fromVisitorStatus(status: com.communityos.visitors.model.VisitorStatus): String {
        return status.name
    }

    @TypeConverter
    fun toVisitorStatus(value: String?): com.communityos.visitors.model.VisitorStatus {
        return com.communityos.visitors.model.VisitorStatus.fromString(value)
    }

    @TypeConverter
    fun fromMarketplaceCategory(category: com.communityos.marketplace.model.MarketplaceCategory): String {
        return category.name
    }

    @TypeConverter
    fun toMarketplaceCategory(value: String?): com.communityos.marketplace.model.MarketplaceCategory {
        return com.communityos.marketplace.model.MarketplaceCategory.fromString(value ?: "")
    }

    @TypeConverter
    fun fromListingStatus(status: com.communityos.marketplace.model.ListingStatus): String {
        return status.name
    }

    @TypeConverter
    fun toListingStatus(value: String?): com.communityos.marketplace.model.ListingStatus {
        return com.communityos.marketplace.model.ListingStatus.fromString(value ?: "")
    }

    @TypeConverter
    fun fromBillStatus(status: com.communityos.maintenance.model.BillStatus): String {
        return status.name
    }

    @TypeConverter
    fun toBillStatus(value: String?): com.communityos.maintenance.model.BillStatus {
        return com.communityos.maintenance.model.BillStatus.fromString(value ?: "")
    }

    @TypeConverter
    fun fromPaymentMethod(method: com.communityos.maintenance.model.PaymentMethod): String {
        return method.name
    }

    @TypeConverter
    fun toPaymentMethod(value: String?): com.communityos.maintenance.model.PaymentMethod {
        return com.communityos.maintenance.model.PaymentMethod.fromString(value ?: "")
    }
}
