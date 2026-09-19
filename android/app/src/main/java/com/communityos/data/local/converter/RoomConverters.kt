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
}
